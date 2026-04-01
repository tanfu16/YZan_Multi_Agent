package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.LayoutAnalysisResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 空间规划顾问
 * 从空间布局角度给方案建议
 * 流程：
 * 标准化输入传给 LLM
 * LLM 返回回答
 * 用 Agent 对应的 Result 类封装回答的信息
 * 整合成统一的 AgentResult
 */
@Component
public class LayoutAgent implements DecorationAgent{

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public LayoutAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentResult execute(StructuredRequirement requirement) {
        try {
            String prompt = buildPrompt(requirement);
            String response = qwenChatModel.chat(prompt);
            LayoutAnalysisResult analysisResult = parseLlmResponse(response);
            return buildAgentResult(analysisResult);
        } catch (Exception e) {
            return fallbackResult();
        }
    }

    private String buildPrompt(StructuredRequirement requirement) {
        return """
                你是一个装修空间布局顾问。
                你的任务是根据用户的结构化装修需求，输出固定 JSON 格式的布局分析结果。
                
                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - recommendations
                   - risks
                   - summary
                
                字段要求：
                - recommendations: 数组，给出 2 到 4 条具体布局建议
                - risks: 数组，给出 1 到 3 条布局相关风险提示
                - summary: 一句话总结整体布局建议
                
                请重点关注：
                - 空间通透感
                - 动线合理性
                - 家庭结构适配
                - 功能区安排
                
                用户结构化需求如下：
                houseType: %s
                area: %s
                budget: %s
                familyProfile: %s
                stylePreference: %s
                priorities: %s
                constraints: %s
                """.formatted(
                safe(requirement.getHouseType()),
                safe(requirement.getArea()),
                safe(requirement.getBudget()),
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints())
        );
    }

    private LayoutAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, LayoutAnalysisResult.class);
    }

    private AgentResult buildAgentResult(LayoutAnalysisResult analysisResult) {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.LAYOUT);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(
                analysisResult.getRecommendations() != null
                        ? analysisResult.getRecommendations()
                        : List.of("建议优化整体空间布局")
        );
        result.setRisks(
                analysisResult.getRisks() != null
                        ? analysisResult.getRisks()
                        : List.of("当前布局分析结果不完整")
        );
        result.setSummary(
                analysisResult.getSummary() != null
                        ? analysisResult.getSummary()
                        : "整体布局建议偏向兼顾空间感与实用性"
        );
        return result;
    }

    private AgentResult fallbackResult() {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.LAYOUT);
        result.setAgentExecutionStatus(AgentExecutionStatus.DEGRADED);
        result.setRecommendations(List.of("建议采用开放式客餐厅布局，提升空间通透感"));
        result.setRisks(List.of("如果收纳不足，开放式布局容易显得杂乱"));
        result.setSummary("布局方案偏向提升空间感和通透性");
        return result;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
