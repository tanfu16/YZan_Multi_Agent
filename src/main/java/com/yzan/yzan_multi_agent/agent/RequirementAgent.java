package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.RequirementExtractionResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 需求整理员
 * 把用户原始需求整理成结构化需求。
 * 这里通过 LangChain4j 的 @MemoryId + ChatMemoryProvider
 * 在同一 session 内自动携带历史上下文。
 */
@Component
public class RequirementAgent {

    private final ObjectMapper objectMapper;
    private final RequirementMemoryAssistant requirementMemoryAssistant;

    public RequirementAgent(QwenChatModel qwenChatModel,
                            ObjectMapper objectMapper,
                            ChatMemoryProvider chatMemoryProvider) {
        this.objectMapper = objectMapper;
        this.requirementMemoryAssistant = AiServices.builder(RequirementMemoryAssistant.class)
                .chatModel(qwenChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    public StructuredRequirement execute(UserRequirement userRequirement) {
        try {
            String memoryId = resolveMemoryId(userRequirement);
            String prompt = buildPrompt(userRequirement);
            String response = requirementMemoryAssistant.extract(memoryId, prompt);
            RequirementExtractionResult extractionResult = parseLlmResponse(response);
            return buildStructuredRequirement(userRequirement, extractionResult);
        } catch (Exception e) {
            System.out.println("RequirementAgent: using fallback path");
            e.printStackTrace();
            return fallbackToRuleBased(userRequirement);
        }
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

    private String buildPrompt(UserRequirement userRequirement) {
        return """
                请基于当前输入以及同一会话中的历史上下文，提取并整理出结构化装修需求。
                如果本轮输入出现“继续上一轮”“还是原来的风格”“预算改一下”之类表达，请结合历史上下文补全缺失信息。

                只输出合法 JSON，不要输出 markdown，不要输出解释。

                JSON 字段固定为：
                - familyProfile
                - stylePreference
                - priorities
                - constraints

                字段要求：
                - familyProfile: 用简洁中文概括家庭结构
                - stylePreference: 提炼用户风格偏好
                - priorities: 输出数组，表示核心优先级
                - constraints: 输出数组，表示约束条件

                当前用户输入如下：
                houseType: %s
                area: %s
                budget: %s
                familyMembers: %s
                stylePreference: %s
                specialNeeds: %s
                rawDescription: %s
                """.formatted(
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

    private RequirementExtractionResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, RequirementExtractionResult.class);
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

    private StructuredRequirement buildStructuredRequirement(
            UserRequirement userRequirement,
            RequirementExtractionResult extractionResult
    ) {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType(userRequirement.getHouseType());
        requirement.setArea(userRequirement.getArea());
        requirement.setBudget(userRequirement.getBudget());
        requirement.setFamilyProfile(extractionResult.getFamilyProfile());
        requirement.setStylePreference(
                extractionResult.getStylePreference() != null
                        ? extractionResult.getStylePreference()
                        : userRequirement.getStylePreference()
        );
        requirement.setPriorities(
                extractionResult.getPriorities() != null
                        ? extractionResult.getPriorities()
                        : List.of("实用")
        );
        requirement.setConstraints(
                extractionResult.getConstraints() != null
                        ? extractionResult.getConstraints()
                        : List.of()
        );
        return requirement;
    }

    private StructuredRequirement fallbackToRuleBased(UserRequirement userRequirement) {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType(userRequirement.getHouseType());
        requirement.setArea(userRequirement.getArea());
        requirement.setBudget(userRequirement.getBudget());
        requirement.setStylePreference(userRequirement.getStylePreference());
        requirement.setFamilyProfile(buildFamilyProfile(userRequirement.getFamilyMembers()));
        requirement.setPriorities(buildPriorities(userRequirement));
        requirement.setConstraints(buildConstraints(userRequirement));
        return requirement;
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

    interface RequirementMemoryAssistant {

        @SystemMessage("你是一个装修需求结构化助手。你的任务是根据当前用户输入和同一会话中的历史上下文，输出固定 JSON。不要输出 markdown，不要输出解释，不要输出 JSON 以外的内容。")
        String extract(@MemoryId String memoryId, @UserMessage String prompt);
    }
}
