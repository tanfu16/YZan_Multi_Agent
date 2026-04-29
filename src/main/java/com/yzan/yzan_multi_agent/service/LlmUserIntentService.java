package com.yzan.yzan_multi_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.IntentClassificationRequest;
import com.yzan.yzan_multi_agent.domain.IntentClassificationResult;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LlmUserIntentService implements UserIntentService {

    private static final List<String> DECORATION_KEYWORDS = List.of(
            "装修", "设计", "方案", "改造", "翻新", "软装", "硬装", "全屋",
            "户型", "客厅", "卧室", "厨房", "卫生间", "阳台", "玄关", "儿童房", "老人房",
            "一室", "二室", "两室", "三室", "四室", "五室", "预算", "风格", "收纳", "动线",
            "防滑", "耐磨", "好打理", "宠物", "孩子", "老人", "平米", "平方米", "㎡"
    );

    private static final List<String> MODIFICATION_KEYWORDS = List.of(
            "改成", "换成", "调整", "修改", "不要", "去掉", "增加", "减少", "还是", "保持",
            "预算改", "面积改", "风格改", "上一版", "刚才", "前面", "方案里", "这个方案"
    );

    private static final List<String> MATERIAL_SKILL_KEYWORDS = List.of(
            "哪里买", "附近", "门店", "建材市场", "材料店", "购买地址", "地砖", "瓷砖",
            "乳胶漆", "板材", "五金", "地板", "灯具"
    );

    private static final List<String> FURNITURE_SKILL_KEYWORDS = List.of(
            "搜", "搜索", "找几款", "候选商品", "电商", "京东", "淘宝", "沙发", "餐桌",
            "床", "衣柜", "书柜", "椅子", "落地灯"
    );

    private static final List<String> SMALL_TALK_KEYWORDS = List.of(
            "你好", "您好", "嗨", "hi", "hello", "在吗", "谢谢", "感谢", "早上好", "下午好", "晚上好"
    );

    private final ObjectMapper objectMapper;
    private final IntentMemoryAssistant intentMemoryAssistant;

    @Autowired
    public LlmUserIntentService(QwenChatModel qwenChatModel,
                                ChatMemoryProvider chatMemoryProvider,
                                ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.intentMemoryAssistant = AiServices.builder(IntentMemoryAssistant.class)
                .chatModel(qwenChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    LlmUserIntentService(ObjectMapper objectMapper, IntentMemoryAssistant intentMemoryAssistant) {
        this.objectMapper = objectMapper;
        this.intentMemoryAssistant = intentMemoryAssistant;
    }

    @Override
    public IntentClassificationResult classify(IntentClassificationRequest request) {
        String userRequest = safe(request == null ? null : request.getUserRequest());
        IntentClassificationResult fastResult = fastClassify(userRequest);
        if (fastResult != null) {
            return fastResult;
        }

        String sessionId = resolveSessionId(request);

        try {
            String response = intentMemoryAssistant.classify(sessionId, buildPrompt(userRequest));
            IntentClassificationResult result = objectMapper.readValue(response, IntentClassificationResult.class);
            return normalizeResult(result, userRequest);
        } catch (Exception e) {
            System.out.println("LlmUserIntentService: using rule-based fallback, reason = " + e.getMessage());
            return fallbackClassify(userRequest);
        }
    }

    private String resolveSessionId(IntentClassificationRequest request) {
        if (request != null && request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return "intent:" + request.getSessionId().trim();
        }
        return "intent:" + UUID.randomUUID();
    }

    private String buildPrompt(String userRequest) {
        return """
                请判断当前用户输入的真实意图，并结合本会话的历史上下文。

                你必须特别处理“追问/修改需求”：
                - 如果历史中已经讨论过装修方案，当前输入如“预算改成20万”“不要开放式厨房”“还是现代简约”“把儿童房收纳加强”，应识别为 PLAN_MODIFICATION。
                - 如果当前输入本身包含完整装修需求，如户型、面积、预算、风格、家庭成员、收纳/安全/清洁诉求，应识别为 PLAN_GENERATION。
                - 如果是在找附近建材门店或购买地点，识别为 MATERIAL_STORE_SKILL。
                - 如果是在京东/淘宝等平台找家具商品，识别为 FURNITURE_SEARCH_SKILL。
                - 如果只是问候、感谢、闲聊，识别为 GENERAL_CHAT。
                - 如果用户似乎想做装修相关事情但信息不足且历史也无法补全，识别为 CLARIFICATION。

                只输出合法 JSON，不要 markdown，不要解释。
                JSON 字段固定为：
                {
                  "intent": "PLAN_GENERATION | PLAN_MODIFICATION | MATERIAL_STORE_SKILL | FURNITURE_SEARCH_SKILL | GENERAL_CHAT | CLARIFICATION",
                  "confidence": 0.0,
                  "reason": "一句中文原因",
                  "requirementModification": false,
                  "normalizedUserRequest": "整理后的用户请求",
                  "skillType": "material-store | furniture-search | none",
                  "reply": "如果是 GENERAL_CHAT 或 CLARIFICATION，给用户的一句简短回复；其他意图为空字符串"
                }

                当前用户输入：
                %s
                """.formatted(userRequest);
    }

    private IntentClassificationResult normalizeResult(IntentClassificationResult result, String userRequest) {
        if (result == null || result.getIntent() == null || result.getIntent().isBlank()) {
            return fallbackClassify(userRequest);
        }

        result.setIntent(result.getIntent().trim().toUpperCase(Locale.ROOT));
        if (result.getConfidence() < 0 || result.getConfidence() > 1) {
            result.setConfidence(0.5);
        }
        if (result.getNormalizedUserRequest() == null || result.getNormalizedUserRequest().isBlank()) {
            result.setNormalizedUserRequest(userRequest);
        }
        if (result.getSkillType() == null || result.getSkillType().isBlank()) {
            result.setSkillType("none");
        }
        if (PLAN_MODIFICATION.equals(result.getIntent())) {
            result.setRequirementModification(true);
        }
        if ((GENERAL_CHAT.equals(result.getIntent()) || CLARIFICATION.equals(result.getIntent()))
                && (result.getReply() == null || result.getReply().isBlank())) {
            result.setReply(defaultReply(result.getIntent()));
        }
        return result;
    }

    private IntentClassificationResult fastClassify(String userRequest) {
        String normalized = userRequest.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || isOnlySmallTalk(normalized)) {
            return result(
                    GENERAL_CHAT,
                    0.98,
                    "规则快速识别为问候或闲聊。",
                    false,
                    userRequest,
                    "none",
                    defaultReply(GENERAL_CHAT)
            );
        }

        if (containsAny(normalized, MODIFICATION_KEYWORDS)) {
            return result(
                    PLAN_MODIFICATION,
                    0.85,
                    "规则快速识别为对历史装修需求或方案的修改。",
                    true,
                    userRequest,
                    "none",
                    ""
            );
        }

        if (containsAny(normalized, MATERIAL_SKILL_KEYWORDS)) {
            return result(
                    MATERIAL_STORE_SKILL,
                    0.9,
                    "规则快速识别为建材门店或附近购买意图。",
                    false,
                    userRequest,
                    "material-store",
                    ""
            );
        }

        if (containsAny(normalized, FURNITURE_SKILL_KEYWORDS)) {
            return result(
                    FURNITURE_SEARCH_SKILL,
                    0.9,
                    "规则快速识别为线上家具商品搜索意图。",
                    false,
                    userRequest,
                    "furniture-search",
                    ""
            );
        }

        if (hasAreaOrBudget(normalized) || looksLikeCompletePlanRequest(normalized)) {
            return result(
                    PLAN_GENERATION,
                    0.85,
                    "规则快速识别为装修方案生成需求。",
                    false,
                    userRequest,
                    "none",
                    ""
            );
        }

        return null;
    }

    private IntentClassificationResult result(String intent,
                                              double confidence,
                                              String reason,
                                              boolean requirementModification,
                                              String normalizedUserRequest,
                                              String skillType,
                                              String reply) {
        IntentClassificationResult result = new IntentClassificationResult();
        result.setIntent(intent);
        result.setConfidence(confidence);
        result.setReason(reason);
        result.setRequirementModification(requirementModification);
        result.setNormalizedUserRequest(normalizedUserRequest);
        result.setSkillType(skillType);
        result.setReply(reply);
        return result;
    }

    private IntentClassificationResult fallbackClassify(String userRequest) {
        String normalized = userRequest.trim().toLowerCase(Locale.ROOT);
        IntentClassificationResult result = new IntentClassificationResult();
        result.setNormalizedUserRequest(userRequest);
        result.setSkillType("none");

        if (normalized.isBlank() || isOnlySmallTalk(normalized)) {
            result.setIntent(GENERAL_CHAT);
            result.setConfidence(0.7);
            result.setReason("输入是问候或闲聊。");
            result.setReply(defaultReply(GENERAL_CHAT));
            return result;
        }

        if (containsAny(normalized, MATERIAL_SKILL_KEYWORDS)) {
            result.setIntent(MATERIAL_STORE_SKILL);
            result.setConfidence(0.75);
            result.setReason("输入包含建材门店或附近购买意图。");
            result.setSkillType("material-store");
            return result;
        }

        if (containsAny(normalized, FURNITURE_SKILL_KEYWORDS)) {
            result.setIntent(FURNITURE_SEARCH_SKILL);
            result.setConfidence(0.75);
            result.setReason("输入包含线上家具商品搜索意图。");
            result.setSkillType("furniture-search");
            return result;
        }

        if (containsAny(normalized, MODIFICATION_KEYWORDS)) {
            result.setIntent(PLAN_MODIFICATION);
            result.setConfidence(0.65);
            result.setReason("输入包含修改上一轮装修需求的表达。");
            result.setRequirementModification(true);
            return result;
        }

        if (containsAny(normalized, DECORATION_KEYWORDS) || hasAreaOrBudget(normalized)) {
            result.setIntent(PLAN_GENERATION);
            result.setConfidence(0.7);
            result.setReason("输入包含装修方案相关信息。");
            return result;
        }

        result.setIntent(CLARIFICATION);
        result.setConfidence(0.5);
        result.setReason("输入意图不明确，需要用户补充。");
        result.setReply(defaultReply(CLARIFICATION));
        return result;
    }

    private String defaultReply(String intent) {
        if (CLARIFICATION.equals(intent)) {
            return "我还不确定你是想调整装修方案、找建材门店，还是查家具商品。可以再补一句你的目标吗？";
        }
        return "你好，我可以帮你生成装修方案，也可以根据历史对话继续调整需求。";
    }

    private boolean isOnlySmallTalk(String text) {
        String compact = text.replaceAll("[\\s，。,.!！?？~～呀啊呢哈]+", "");
        if (compact.isBlank()) {
            return true;
        }
        return SMALL_TALK_KEYWORDS.contains(compact) || (compact.length() <= 6 && containsAny(compact, SMALL_TALK_KEYWORDS));
    }

    private boolean hasAreaOrBudget(String text) {
        return text.matches(".*\\d{2,3}\\s*(平|平方米|㎡).*")
                || text.matches(".*\\d+(\\.\\d+)?\\s*万.*")
                || text.matches(".*预算[^0-9]*\\d{4,8}.*");
    }

    private boolean looksLikeCompletePlanRequest(String text) {
        int signalCount = 0;
        if (containsAny(text, DECORATION_KEYWORDS)) {
            signalCount++;
        }
        if (text.matches(".*[一二三四五六七八九十0-9]+室.*")) {
            signalCount++;
        }
        if (containsAny(text, List.of("收纳", "防滑", "耐磨", "好打理", "宠物", "孩子", "老人"))) {
            signalCount++;
        }
        if (containsAny(text, List.of("现代简约", "现代原木", "原木风", "奶油风", "中古风", "极简风", "北欧风"))) {
            signalCount++;
        }
        return signalCount >= 2;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    interface IntentMemoryAssistant {

        @SystemMessage("你是用户意图分类器。必须结合当前输入和同一会话历史上下文判断真实意图，尤其要识别用户对历史装修需求或方案的增删改。只输出合法 JSON。")
        String classify(@MemoryId String memoryId, @UserMessage String prompt);
    }
}
