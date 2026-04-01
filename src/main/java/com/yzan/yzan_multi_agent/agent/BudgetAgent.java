package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.BudgetAnalysisResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预算规划顾问
 * 从预算角度给方案建议
 * 流程：
 * 标准化输入传给 LLM
 * LLM 返回回答
 * 用 Agent 对应的 Result 类封装回答的信息
 * 整合成统一的 AgentResult
 */
@Component
public class BudgetAgent implements DecorationAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public BudgetAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentResult execute(StructuredRequirement requirement) {
        try {
            String prompt = buildPrompt(requirement);
            String response = qwenChatModel.chat(prompt);
            BudgetAnalysisResult analysisResult = parseLlmResponse(response);
            return buildAgentResult(analysisResult);
        } catch (Exception e) {
            return fallbackResult();
        }
    }

    private String buildPrompt(StructuredRequirement requirement) {
        return """
                你是一个家庭装修预算顾问。
                你的任务是根据用户的结构化装修需求，输出固定 JSON 格式的预算分析结果。
                
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
                
                请重点关注：
                - 当前预算是否能支撑用户需求
                - 哪些方向容易超预算
                - 哪些方面适合控制成本
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
                safe(requirement.getHouseType()),
                safe(requirement.getArea()),
                safe(requirement.getBudget()),
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints())
        );
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
