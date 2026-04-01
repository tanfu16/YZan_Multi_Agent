package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.SafetyAnalysisResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全规划顾问
 * 从安全角度给方案建议
 * 流程：
 * 标准化输入传给 LLM
 * LLM 返回回答
 * 用 Agent 对应的 Result 类封装回答的信息
 * 整合成统一的 AgentResult
 */
@Component
public class SafetyAgent implements DecorationAgent{

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public SafetyAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentResult execute(StructuredRequirement requirement) {
        try {
            String prompt = buildPrompt(requirement);
            String response = qwenChatModel.chat(prompt);
            SafetyAnalysisResult analysisResult = parseLlmResponse(response);
            return buildAgentResult(analysisResult);
        } catch (Exception e) {
            return fallbackResult();
        }
    }

    private String buildPrompt(StructuredRequirement requirement) {
        return """
                你是一个家庭装修安全顾问。
                你的任务是根据用户的结构化装修需求，输出固定 JSON 格式的安全分析结果。
                
                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - recommendations
                   - risks
                   - summary
                
                字段要求：
                - recommendations: 数组，给出 2 到 4 条安全相关建议
                - risks: 数组，给出 1 到 3 条安全相关风险提示
                - summary: 一句话总结整体安全建议
                
                请重点关注：
                - 儿童、老人、宠物等家庭成员适配
                - 家具尖角、地面防滑、材料耐脏耐磨
                - 日常使用安全与清洁维护
                - 是否存在潜在碰撞、滑倒、损坏风险
                
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

    private SafetyAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, SafetyAnalysisResult.class);
    }

    private AgentResult buildAgentResult(SafetyAnalysisResult analysisResult) {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.SAFETY);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(
                analysisResult.getRecommendations() != null
                        ? analysisResult.getRecommendations()
                        : List.of("建议优先考虑家庭日常使用安全")
        );
        result.setRisks(
                analysisResult.getRisks() != null
                        ? analysisResult.getRisks()
                        : List.of("当前安全分析结果不完整")
        );
        result.setSummary(
                analysisResult.getSummary() != null
                        ? analysisResult.getSummary()
                        : "整体安全建议偏向家庭适配与日常风险控制"
        );
        return result;
    }

    private AgentResult fallbackResult() {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.SAFETY);
        result.setAgentExecutionStatus(AgentExecutionStatus.DEGRADED);
        result.setRecommendations(List.of("建议采用圆角家具，提升儿童和家庭成员使用安全性"));
        result.setRisks(List.of("尖角家具和湿滑地面可能增加碰撞和滑倒风险"));
        result.setSummary("安全建议优先考虑圆角、防滑和家庭成员适配");
        return result;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
