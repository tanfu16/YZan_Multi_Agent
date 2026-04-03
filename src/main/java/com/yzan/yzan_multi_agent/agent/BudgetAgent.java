package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.BudgetAnalysisResult;
import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import com.yzan.yzan_multi_agent.knowledge.KnowledgeRetrievalService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BudgetAgent implements DecorationAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public BudgetAgent(QwenChatModel qwenChatModel,
                       ObjectMapper objectMapper,
                       KnowledgeRetrievalService knowledgeRetrievalService) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public AgentResult execute(StructuredRequirement requirement) {
        try {
            List<KnowledgeChunk> chunks = knowledgeRetrievalService.retrieveForBudget(requirement);
            String prompt = buildPrompt(requirement, chunks);
            String response = qwenChatModel.chat(prompt);
            BudgetAnalysisResult analysisResult = parseLlmResponse(response);
            return buildAgentResult(analysisResult);
        } catch (Exception e) {
            return fallbackResult();
        }
    }

    private String buildPrompt(StructuredRequirement requirement, List<KnowledgeChunk> chunks) {
        String knowledgeText = formatKnowledge(chunks);

        return """
                你是一个家庭装修预算顾问。
                你的任务是根据用户的结构化装修需求，并优先参考系统检索到的装修预算知识，输出固定 JSON 格式的预算分析结果。

                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - recommendations
                   - risks
                   - summary

                字段要求：
                - recommendations: 数组，给出 2 到 4 条预算相关建议
                - risks: 数组，给出 1 到 3 条预算相关风险提示
                - summary: 一句话总结整体预算建议

                请优先参考以下检索到的装修预算知识：
                %s

                请重点关注：
                - 当前预算是否能支撑用户核心需求
                - 哪些方向容易超预算
                - 哪些部分适合控制成本
                - 如何在风格、收纳、安全和实用性之间平衡预算

                用户结构化需求如下：
                houseType: %s
                area: %s
                budget: %s
                familyProfile: %s
                stylePreference: %s
                priorities: %s
                constraints: %s
                """.formatted(
                knowledgeText,
                safe(requirement.getHouseType()),
                safe(requirement.getArea()),
                safe(requirement.getBudget()),
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints())
        );
    }

    private String formatKnowledge(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "暂无可用知识片段。";
        }

        StringBuilder builder = new StringBuilder();
        for (KnowledgeChunk chunk : chunks) {
            builder.append("来源: ")
                    .append(chunk.getSourceName())
                    .append("\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }
        return builder.toString();
    }

    private BudgetAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, BudgetAnalysisResult.class);
    }

    private AgentResult buildAgentResult(BudgetAnalysisResult analysisResult) {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.BUDGET);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(
                analysisResult.getRecommendations() != null
                        ? analysisResult.getRecommendations()
                        : List.of("建议优先保证核心功能区预算投入")
        );
        result.setRisks(
                analysisResult.getRisks() != null
                        ? analysisResult.getRisks()
                        : List.of("当前预算分析结果不完整")
        );
        result.setSummary(
                analysisResult.getSummary() != null
                        ? analysisResult.getSummary()
                        : "整体预算建议偏向控制风险并保证实用性"
        );
        return result;
    }

    private AgentResult fallbackResult() {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.BUDGET);
        result.setAgentExecutionStatus(AgentExecutionStatus.DEGRADED);
        result.setRecommendations(List.of("建议优先控制非核心装饰预算，保证主要功能需求"));
        result.setRisks(List.of("如果同时追求高风格化、高收纳和高配置，预算可能超支"));
        result.setSummary("预算建议优先保证核心需求，适当控制装饰性投入");
        return result;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}