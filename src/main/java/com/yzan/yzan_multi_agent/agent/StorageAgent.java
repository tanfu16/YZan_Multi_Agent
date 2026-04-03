package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StorageAnalysisResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import com.yzan.yzan_multi_agent.knowledge.KnowledgeRetrievalService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StorageAgent implements DecorationAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public StorageAgent(QwenChatModel qwenChatModel,
                        ObjectMapper objectMapper,
                        KnowledgeRetrievalService knowledgeRetrievalService) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public AgentResult execute(StructuredRequirement requirement) {
        try {
            List<KnowledgeChunk> chunks = knowledgeRetrievalService.retrieveForStorage(requirement);
            String prompt = buildPrompt(requirement, chunks);
            String response = qwenChatModel.chat(prompt);
            StorageAnalysisResult analysisResult = parseLlmResponse(response);
            return buildAgentResult(analysisResult);
        } catch (Exception e) {
            return fallbackResult();
        }
    }

    private String buildPrompt(StructuredRequirement requirement, List<KnowledgeChunk> chunks) {
        String knowledgeText = formatKnowledge(chunks);

        return """
                你是一个家庭装修收纳规划顾问。
                你的任务是根据用户的结构化装修需求，并优先参考系统检索到的收纳规划知识，输出固定 JSON 格式的收纳分析结果。

                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - recommendations
                   - risks
                   - summary

                字段要求：
                - recommendations: 数组，给出 2 到 4 条收纳相关建议
                - risks: 数组，给出 1 到 3 条收纳相关风险提示
                - summary: 一句话总结整体收纳建议

                请优先参考以下检索到的收纳规划知识：
                %s

                请重点关注：
                - 玄关、客厅、卧室等空间的柜体与储物能力
                - 开放格与封闭柜体的平衡
                - 收纳需求与空间通透感的关系
                - 是否满足家庭长期整理和使用习惯

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

    private StorageAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, StorageAnalysisResult.class);
    }

    private AgentResult buildAgentResult(StorageAnalysisResult analysisResult) {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.STORAGE);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(
                analysisResult.getRecommendations() != null
                        ? analysisResult.getRecommendations()
                        : List.of("建议增加基础收纳柜体，提升日常整理能力")
        );
        result.setRisks(
                analysisResult.getRisks() != null
                        ? analysisResult.getRisks()
                        : List.of("当前收纳分析结果不完整")
        );
        result.setSummary(
                analysisResult.getSummary() != null
                        ? analysisResult.getSummary()
                        : "整体收纳建议偏向提升储物能力与生活便利性"
        );
        return result;
    }

    private AgentResult fallbackResult() {
        AgentResult result = new AgentResult();
        result.setAgentType(AgentType.STORAGE);
        result.setAgentExecutionStatus(AgentExecutionStatus.DEGRADED);
        result.setRecommendations(List.of("建议增加储物柜，保证日常收纳空间充足"));
        result.setRisks(List.of("柜体过多可能压缩空间感，影响整体通透性"));
        result.setSummary("收纳建议优先考虑储物能力与空间平衡");
        return result;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}