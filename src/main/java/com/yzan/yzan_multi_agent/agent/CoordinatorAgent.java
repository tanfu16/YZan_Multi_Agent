package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.*;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 方案总控
 * 汇总多个专业 agent 的结果，识别冲突并生成最终方案
 * 流程：
 * 多个 Agent 的 AgentResult 输入 LLM
 * LLM 回复包装成分析结果
 * 分析结果包装成最终的计划方案
 */
@Component
public class CoordinatorAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public CoordinatorAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    public DecorationPlan execute(StructuredRequirement requirement, List<AgentResult> results) {
        try {
            String prompt = buildPrompt(requirement, results);
            String response = qwenChatModel.chat(prompt);
            CoordinatorAnalysisResult analysisResult = parseLlmResponse(response);
            return buildDecorationPlan(analysisResult);
        } catch (Exception e) {
            return fallbackPlan(requirement, results);
        }
    }

    private String buildPrompt(StructuredRequirement requirement, List<AgentResult> results) {
        return """
                你是一个装修方案总协调顾问。
                你的任务是根据用户的结构化装修需求，以及多个专业 agent 的分析结果，
                汇总出最终装修方案，并以固定 JSON 格式输出。
                
                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - summary
                   - keyRecommendations
                   - conflicts
                   - finalSuggestion
                
                字段要求：
                - summary: 一句话总结整体方案
                - keyRecommendations: 数组，给出 3 到 6 条关键建议
                - conflicts: 数组，每个元素包含：
                  - topic
                  - relatedAgents
                  - description
                  - resolution
                - finalSuggestion: 一句话给出最终推荐方向
                
                请重点关注：
                - 多个专业 agent 是否存在观点冲突
                - 如何在布局、预算、安全、收纳之间做平衡
                - 最终方案是否兼顾家庭需求与可落地性
                
                用户结构化需求如下：
                houseType: %s
                area: %s
                budget: %s
                familyProfile: %s
                stylePreference: %s
                priorities: %s
                constraints: %s
                
                专业 agent 结果如下：
                %s
                """.formatted(
                safe(requirement.getHouseType()),
                safe(requirement.getArea()),
                safe(requirement.getBudget()),
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints()),
                formatAgentResults(results)
        );
    }

    private CoordinatorAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, CoordinatorAnalysisResult.class);
    }

    private DecorationPlan buildDecorationPlan(CoordinatorAnalysisResult analysisResult) {
        DecorationPlan plan = new DecorationPlan();
        plan.setSummary(analysisResult.getSummary());
        plan.setKeyRecommendations(
                analysisResult.getKeyRecommendations() != null
                        ? analysisResult.getKeyRecommendations()
                        : List.of("建议综合平衡空间、预算与家庭适配性")
        );
        plan.setConflicts(
                analysisResult.getConflicts() != null
                        ? analysisResult.getConflicts()
                        : List.of()
        );
        plan.setFinalSuggestion(
                analysisResult.getFinalSuggestion() != null
                        ? analysisResult.getFinalSuggestion()
                        : "建议采用兼顾实用性与舒适度的综合方案"
        );
        return plan;
    }

    private DecorationPlan fallbackPlan(StructuredRequirement requirement, List<AgentResult> results) {
        DecorationPlan plan = new DecorationPlan();

        List<String> recommendations = collectRecommendations(results);
        List<ConflictItem> conflicts = detectConflicts(results);

        plan.setSummary("该方案基于户型、预算、家庭结构和风格偏好进行了多 agent 协同分析。");
        plan.setKeyRecommendations(recommendations);
        plan.setConflicts(conflicts);
        plan.setFinalSuggestion(
                conflicts.isEmpty()
                        ? "建议直接采用综合方案，优先兼顾实用性与舒适度。"
                        : "建议采用平衡型方案，在通透性、美观和家庭安全之间做折中。"
        );

        return plan;
    }

    private List<String> collectRecommendations(List<AgentResult> results) {
        List<String> recommendations = new ArrayList<>();
        for (AgentResult result : results) {
            if (result.getRecommendations() != null) {
                recommendations.addAll(result.getRecommendations());
            }
        }
        return recommendations;
    }

    private List<ConflictItem> detectConflicts(List<AgentResult> results) {
        List<ConflictItem> conflicts = new ArrayList<>();

        boolean hasLayout = false;
        boolean hasSafety = false;

        for (AgentResult result : results) {
            if (result.getAgentType() == AgentType.LAYOUT) {
                hasLayout = true;
            }
            if (result.getAgentType() == AgentType.SAFETY) {
                hasSafety = true;
            }
        }

        if (hasLayout && hasSafety) {
            ConflictItem conflictItem = new ConflictItem();
            conflictItem.setTopic("空间通透性与家庭安全性冲突");
            conflictItem.setRelatedAgents(List.of("LAYOUT", "SAFETY"));
            conflictItem.setDescription("开放式布局更通透，但儿童或宠物家庭更需要控制尖角和开放区域风险。");
            conflictItem.setResolution("保留空间通透感，同时采用圆角家具和更安全的柜体设计。");
            conflicts.add(conflictItem);
        }

        return conflicts;
    }

    private String formatAgentResults(List<AgentResult> results) {
        StringBuilder builder = new StringBuilder();

        for (AgentResult result : results) {
            builder.append("agentType: ").append(result.getAgentType()).append("\n");
            builder.append("status: ").append(result.getAgentExecutionStatus()).append("\n");
            builder.append("recommendations: ").append(result.getRecommendations()).append("\n");
            builder.append("risks: ").append(result.getRisks()).append("\n");
            builder.append("summary: ").append(result.getSummary()).append("\n");
            builder.append("\n");
        }

        return builder.toString();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}