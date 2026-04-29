package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.ScalarFieldPatch;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.StructuredRequirementPatch;
import com.yzan.yzan_multi_agent.domain.StringListPatch;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.service.RequirementStateService;
import com.yzan.yzan_multi_agent.service.RequirementUnderstandingService;
import com.yzan.yzan_multi_agent.service.StructuredRequirementStateMerger;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import com.yzan.yzan_multi_agent.service.PlanIntentService;
import com.yzan.yzan_multi_agent.service.ConversationMemoryService;
import com.yzan.yzan_multi_agent.skills.SkillIntentRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * 需求整理员
 * 把用户原始需求整理成结构化需求。
 * 这里通过 LangChain4j 的 @MemoryId + ChatMemoryProvider
 * 在同一 session 内自动携带历史上下文。
 */
@Component
public class RequirementAgent implements RequirementUnderstandingService {

    private final ObjectMapper objectMapper;
    private final RequirementMemoryAssistant requirementMemoryAssistant;
    private final SkillIntentRouter skillIntentRouter;
    private final PlanIntentService planIntentService;
    private final RequirementStateService requirementStateService;
    private final StructuredRequirementStateMerger structuredRequirementStateMerger;
    private final ConversationMemoryService conversationMemoryService;

    @Autowired
    public RequirementAgent(QwenChatModel qwenChatModel,
                            ObjectMapper objectMapper,
                            ChatMemoryProvider chatMemoryProvider,
                            SkillIntentRouter skillIntentRouter,
                            PlanIntentService planIntentService,
                            RequirementStateService requirementStateService,
                            StructuredRequirementStateMerger structuredRequirementStateMerger,
                            ConversationMemoryService conversationMemoryService) {
        this.objectMapper = objectMapper;
        this.skillIntentRouter = skillIntentRouter;
        this.planIntentService = planIntentService;
        this.requirementStateService = requirementStateService;
        this.structuredRequirementStateMerger = structuredRequirementStateMerger;
        this.conversationMemoryService = conversationMemoryService;
        this.requirementMemoryAssistant = AiServices.builder(RequirementMemoryAssistant.class)
                .chatModel(qwenChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    public RequirementAgent(ObjectMapper objectMapper,
                            RequirementMemoryAssistant requirementMemoryAssistant,
                            SkillIntentRouter skillIntentRouter,
                            PlanIntentService planIntentService,
                            RequirementStateService requirementStateService,
                            StructuredRequirementStateMerger structuredRequirementStateMerger,
                            ConversationMemoryService conversationMemoryService) {
        this.objectMapper = objectMapper;
        this.requirementMemoryAssistant = requirementMemoryAssistant;
        this.skillIntentRouter = skillIntentRouter;
        this.planIntentService = planIntentService;
        this.requirementStateService = requirementStateService;
        this.structuredRequirementStateMerger = structuredRequirementStateMerger;
        this.conversationMemoryService = conversationMemoryService;
    }

    public RequirementUnderstandingResult understand(UserRequirement userRequirement) {
        String memoryId = resolveMemoryId(userRequirement);
        StructuredRequirement currentState = requirementStateService.load(memoryId);
        String latestSummary = conversationMemoryService.loadLatestSummary(memoryId);
        try {
            String prompt = buildPrompt(userRequirement, currentState, latestSummary);
            String response = requirementMemoryAssistant.extract(memoryId, prompt);
            RequirementUnderstandingResult result = parseLlmResponse(response);
            return normalizeResult(userRequirement, currentState, result, memoryId);
        } catch (Exception e) {
            System.out.println("RequirementAgent: using fallback path, reason = " + e.getMessage());
            return fallbackToRuleBased(userRequirement, currentState, memoryId);
        }
    }

    public StructuredRequirement execute(UserRequirement userRequirement) {
        return understand(userRequirement).getStructuredRequirement();
    }

    private String resolveMemoryId(UserRequirement userRequirement) {
        if (userRequirement != null && userRequirement.getSessionId() != null && !userRequirement.getSessionId().isBlank()) {
            return userRequirement.getSessionId().trim();
        }
        String sessionId = UUID.randomUUID().toString();
        if (userRequirement != null) {
            userRequirement.setSessionId(sessionId);
        }
        return sessionId;
    }

    private String buildPrompt(UserRequirement userRequirement,
                               StructuredRequirement currentState,
                               String latestSummary) {
        return """
                请基于当前输入以及同一会话中的必要历史上下文，统一理解用户请求。
                如果本轮输入出现“继续上一轮”“还是原来的风格”“预算改一下”之类表达，请结合历史上下文补全缺失信息。
                下面会给你一份“当前已确认的 StructuredRequirement 状态”，它代表本会话已经确认的装修需求真值。你必须在此基础上判断当前输入是新增、修改、删除还是保持不变。
                另外还会给你一份“长期记忆摘要”，这是系统在历史对话较长时主动压缩出的要点摘要。它比聊天窗口更稳定，可用于辅助理解历史上下文。

                你需要先判断用户请求类型，再按该类型填写固定 JSON。
                只输出合法 JSON，不要输出 markdown，不要输出解释。

                JSON 字段固定为：
                - intentType
                - reply
                - skillName
                - structuredRequirementPatch

                intentType 只能是：
                - GENERAL_CHAT
                - PLAN_GENERATION
                - SKILL_CALL

                分支规则必须严格遵守：
                1. 如果 intentType = GENERAL_CHAT：
                   - 必须直接生成 reply
                   - reply 只写 1 到 2 句，简短、自然、直接回应用户
                   - 不要分析，不要解释系统能力，不要输出多余建议
                   - structuredRequirementPatch 必须为 null
                   - skillName 必须为空字符串

                2. 如果 intentType = PLAN_GENERATION：
                   - reply 必须为空字符串
                   - skillName 必须为空字符串
                   - structuredRequirementPatch 必须填写对象
                   - 不要返回完整 requirement，只返回 patch
                   - 标量字段 operation 只能是 SET / REMOVE / KEEP
                   - 列表字段 operation 只能是 ADD / REMOVE / REPLACE / CLEAR / KEEP
                   - 只有明确修改才使用 SET / REMOVE / ADD / REPLACE / CLEAR
                   - 没提到的字段必须用 KEEP
                   - 模糊表达请降低 confidence，不要高置信覆盖旧状态

                3. 如果 intentType = SKILL_CALL：
                   - reply 必须为空字符串
                   - structuredRequirementPatch 必须为 null
                   - skillName 必须填写 material-store-search-skill 或 furniture-search-skill

                structuredRequirementPatch 中字段固定为：
                - houseType: { operation, value, confidence }
                - area: { operation, value, confidence }
                - budget: { operation, value, confidence }
                - familyProfile: { operation, value, confidence }
                - stylePreference: { operation, value, confidence }
                - priorities: { operation, values, confidence }
                - constraints: { operation, values, confidence }

                识别规则：
                - 普通问候、感谢、寒暄、简单确认、普通闲聊，识别为 GENERAL_CHAT，并直接回复
                - 线下建材门店搜索、线上家具商品搜索，识别为 SKILL_CALL
                - 其他装修需求、方案修改、预算/风格/户型调整，识别为 PLAN_GENERATION
                - 生成方案时，如果原始输入里已有 houseType / area / budget，也应通过 patch 明确表达

                当前已确认的 StructuredRequirement 状态：
                %s

                当前长期记忆摘要：
                %s

                当前用户输入如下：
                houseType: %s
                area: %s
                budget: %s
                familyMembers: %s
                stylePreference: %s
                specialNeeds: %s
                rawDescription: %s
                """.formatted(
                safe(toJson(currentState)),
                safe(latestSummary),
                safe(userRequirement.getHouseType()),
                safe(userRequirement.getArea()),
                safe(userRequirement.getBudget()),
                safe(userRequirement.getFamilyMembers()),
                safe(userRequirement.getStylePreference()),
                safe(userRequirement.getSpecialNeeds()),
                safe(userRequirement.getRawDescription())
        );
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private RequirementUnderstandingResult parseLlmResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        RequirementUnderstandingResult result = new RequirementUnderstandingResult();
        if (root.hasNonNull("intentType")) {
            result.setIntentType(UserIntentType.valueOf(root.path("intentType").asText().trim().toUpperCase(Locale.ROOT)));
        }
        result.setReply(blankToNull(root.path("reply").asText(null)));
        result.setSkillName(blankToNull(root.path("skillName").asText(null)));
        if (root.has("structuredRequirementPatch") && !root.path("structuredRequirementPatch").isNull()) {
            result.setStructuredRequirementPatch(parseStructuredRequirementPatch(root.path("structuredRequirementPatch")));
        }
        return result;
    }

    private StructuredRequirementPatch parseStructuredRequirementPatch(JsonNode node) {
        StructuredRequirementPatch patch = new StructuredRequirementPatch();
        patch.setHouseType(parseStringPatch(node.path("houseType")));
        patch.setArea(parseIntegerPatch(node.path("area")));
        patch.setBudget(parseBudgetPatch(node.path("budget")));
        patch.setFamilyProfile(parseStringPatch(node.path("familyProfile")));
        patch.setStylePreference(parseStringPatch(node.path("stylePreference")));
        patch.setPriorities(parseListPatch(node.path("priorities")));
        patch.setConstraints(parseListPatch(node.path("constraints")));
        return patch;
    }

    private ScalarFieldPatch<String> parseStringPatch(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        ScalarFieldPatch<String> patch = new ScalarFieldPatch<>();
        patch.setOperation(blankToNull(node.path("operation").asText(null)));
        patch.setValue(blankToNull(node.path("value").asText(null)));
        patch.setConfidence(readConfidence(node.path("confidence")));
        return patch;
    }

    private ScalarFieldPatch<Integer> parseIntegerPatch(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        ScalarFieldPatch<Integer> patch = new ScalarFieldPatch<>();
        patch.setOperation(blankToNull(node.path("operation").asText(null)));
        if (node.hasNonNull("value") && node.path("value").canConvertToInt()) {
            patch.setValue(node.path("value").asInt());
        }
        patch.setConfidence(readConfidence(node.path("confidence")));
        return patch;
    }

    private ScalarFieldPatch<BigDecimal> parseBudgetPatch(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        ScalarFieldPatch<BigDecimal> patch = new ScalarFieldPatch<>();
        patch.setOperation(blankToNull(node.path("operation").asText(null)));
        if (node.hasNonNull("value")) {
            String budgetText = blankToNull(node.path("value").asText(null));
            if (budgetText != null) {
                try {
                    patch.setValue(new BigDecimal(budgetText));
                } catch (NumberFormatException ignored) {
                    // Leave null and let merger keep current state.
                }
            }
        }
        patch.setConfidence(readConfidence(node.path("confidence")));
        return patch;
    }

    private StringListPatch parseListPatch(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        StringListPatch patch = new StringListPatch();
        patch.setOperation(blankToNull(node.path("operation").asText(null)));
        patch.setValues(readStringList(node.path("values")));
        patch.setConfidence(readConfidence(node.path("confidence")));
        return patch;
    }

    private Double readConfidence(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.isNumber() ? node.asDouble() : null;
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isNull()) {
            return values;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String text = blankToNull(item.asText(null));
                if (text != null) {
                    values.add(text);
                }
            }
            return values;
        }

        String text = blankToNull(node.asText(null));
        if (text == null) {
            return values;
        }
        for (String part : text.split("[,，/、；;]+")) {
            String trimmed = blankToNull(part);
            if (trimmed != null) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private RequirementUnderstandingResult normalizeResult(UserRequirement userRequirement,
                                                           StructuredRequirement currentState,
                                                           RequirementUnderstandingResult result,
                                                           String memoryId) {
        if (result == null || result.getIntentType() == null) {
            return fallbackToRuleBased(userRequirement, currentState, memoryId);
        }

        result.setRawInput(safe(userRequirement == null ? null : userRequirement.getRawDescription()));
        if (result.getIntentType() == UserIntentType.GENERAL_CHAT) {
            result.setReply(hasText(result.getReply()) ? result.getReply() : buildDefaultChatReply(userRequirement));
            result.setStructuredRequirement(null);
            result.setStructuredRequirementPatch(null);
            result.setSkillName(null);
            return result;
        }

        if (result.getIntentType() == UserIntentType.SKILL_CALL) {
            result.setReply(null);
            result.setStructuredRequirement(null);
            result.setStructuredRequirementPatch(null);
            if (!hasText(result.getSkillName())) {
                result.setSkillName(skillIntentRouter.route(safe(userRequirement == null ? null : userRequirement.getRawDescription()))
                        .orElse(null));
            }
            return result;
        }

        result.setReply(null);
        result.setSkillName(null);
        StructuredRequirement mergedState = structuredRequirementStateMerger.merge(
                currentState,
                result.getStructuredRequirementPatch(),
                userRequirement
        );
        result.setStructuredRequirement(mergedState);
        requirementStateService.save(memoryId, mergedState);
        return result;
    }

    private List<String> buildConstraints(UserRequirement userRequirement) {
        List<String> constraints = new ArrayList<>();

        if (userRequirement.getSpecialNeeds() != null) {
            for (String need : userRequirement.getSpecialNeeds()) {
                if (need.contains("不要复杂吊顶")) {
                    constraints.add("避免复杂吊顶");
                }
                if (need.contains("孩子")) {
                    constraints.add("避免尖角家具");
                }
                if (need.contains("宠物")) {
                    constraints.add("选择耐脏耐磨材料");
                }
            }
        }

        if (userRequirement.getRawDescription() != null) {
            String raw = userRequirement.getRawDescription();
            if (raw.contains("孩子")) {
                constraints.add("避免尖角家具");
            }
            if (raw.contains("宠物") || raw.contains("猫") || raw.contains("狗")) {
                constraints.add("选择耐脏耐磨材料");
            }
        }

        return constraints;
    }

    private RequirementUnderstandingResult fallbackToRuleBased(UserRequirement userRequirement,
                                                               StructuredRequirement currentState,
                                                               String memoryId) {
        RequirementUnderstandingResult result = new RequirementUnderstandingResult();
        result.setRawInput(safe(userRequirement == null ? null : userRequirement.getRawDescription()));

        String rawText = normalize(userRequirement == null ? null : userRequirement.getRawDescription());
        if (isOnlySmallTalk(rawText)) {
            result.setIntentType(UserIntentType.GENERAL_CHAT);
            result.setReply(buildDefaultChatReply(userRequirement));
            return result;
        }

        String skillName = skillIntentRouter.route(userRequirement == null ? null : userRequirement.getRawDescription())
                .orElse(null);
        if (hasText(skillName)) {
            result.setIntentType(UserIntentType.SKILL_CALL);
            result.setSkillName(skillName);
            return result;
        }

        if (!planIntentService.isDecorationPlanIntent(userRequirement)) {
            result.setIntentType(UserIntentType.GENERAL_CHAT);
            result.setReply(buildDefaultChatReply(userRequirement));
            return result;
        }

        StructuredRequirementPatch patch = new StructuredRequirementPatch();
        patch.setHouseType(createStringPatch("SET", userRequirement.getHouseType()));
        patch.setArea(createIntegerPatch("SET", userRequirement.getArea()));
        patch.setBudget(createBigDecimalPatch("SET", userRequirement.getBudget()));
        patch.setFamilyProfile(createStringPatch("SET", buildFamilyProfile(userRequirement.getFamilyMembers())));
        patch.setStylePreference(createStringPatch("SET", userRequirement.getStylePreference()));
        patch.setPriorities(createListPatch("ADD", buildPriorities(userRequirement)));
        patch.setConstraints(createListPatch("ADD", buildConstraints(userRequirement)));
        StructuredRequirement requirement = structuredRequirementStateMerger.merge(currentState, patch, userRequirement);
        result.setIntentType(UserIntentType.PLAN_GENERATION);
        result.setStructuredRequirementPatch(patch);
        result.setStructuredRequirement(requirement);
        requirementStateService.save(memoryId, requirement);
        return result;
    }

    private String buildFamilyProfile(List<String> familyMembers) {
        if (familyMembers == null || familyMembers.isEmpty()) {
            return "未知家庭结构";
        }
        return String.join("+", familyMembers);
    }

    private List<String> buildPriorities(UserRequirement userRequirement) {
        List<String> priorities = new ArrayList<>();

        if (userRequirement.getSpecialNeeds() != null) {
            for (String need : userRequirement.getSpecialNeeds()) {
                if (need.contains("收纳")) {
                    priorities.add("收纳");
                }
                if (need.contains("安全")) {
                    priorities.add("安全");
                }
                if (need.contains("清洁") || need.contains("好打理")) {
                    priorities.add("清洁");
                }
                if (need.contains("通透")) {
                    priorities.add("通透");
                }
            }
        }

        if (priorities.isEmpty()) {
            priorities.add("实用");
        }

        return priorities;
    }

    private String buildDefaultChatReply(UserRequirement userRequirement) {
        String raw = safe(userRequirement == null ? null : userRequirement.getRawDescription());
        if (raw.contains("谢谢") || raw.contains("感谢")) {
            return "不客气。你可以继续告诉我户型、面积、预算和风格，我来帮你整理装修方案。";
        }
        return "你好，你可以直接告诉我户型、面积、预算、风格和家庭成员需求，我来帮你生成装修方案。";
    }

    private boolean isOnlySmallTalk(String text) {
        String compact = text.replaceAll("[\\s，。,.!！?？~～呀啊呢哈]+", "");
        if (compact.isBlank()) {
            return true;
        }
        return containsAny(compact, "你好", "您好", "嗨", "hi", "hello", "在吗", "谢谢", "感谢", "早上好", "下午好", "晚上好");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String blankToNull(String text) {
        return hasText(text) ? text.trim() : null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private ScalarFieldPatch<String> createStringPatch(String operation, String value) {
        if (!hasText(value)) {
            return null;
        }
        ScalarFieldPatch<String> patch = new ScalarFieldPatch<>();
        patch.setOperation(operation);
        patch.setValue(value.trim());
        patch.setConfidence(1.0);
        return patch;
    }

    private ScalarFieldPatch<Integer> createIntegerPatch(String operation, Integer value) {
        if (value == null) {
            return null;
        }
        ScalarFieldPatch<Integer> patch = new ScalarFieldPatch<>();
        patch.setOperation(operation);
        patch.setValue(value);
        patch.setConfidence(1.0);
        return patch;
    }

    private ScalarFieldPatch<BigDecimal> createBigDecimalPatch(String operation, BigDecimal value) {
        if (value == null) {
            return null;
        }
        ScalarFieldPatch<BigDecimal> patch = new ScalarFieldPatch<>();
        patch.setOperation(operation);
        patch.setValue(value);
        patch.setConfidence(1.0);
        return patch;
    }

    private StringListPatch createListPatch(String operation, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringListPatch patch = new StringListPatch();
        patch.setOperation(operation);
        patch.setValues(values);
        patch.setConfidence(1.0);
        return patch;
    }

    interface RequirementMemoryAssistant {

        @SystemMessage("你是一个装修对话入口助手。你的任务是根据当前用户输入和同一会话中的历史上下文，先判断请求类型，再输出固定 JSON。普通对话一旦判断为 GENERAL_CHAT，就直接给出简短 reply。不要输出 markdown，不要输出解释，不要输出 JSON 以外的内容。")
        String extract(@MemoryId String memoryId, @UserMessage String prompt);
    }
}
