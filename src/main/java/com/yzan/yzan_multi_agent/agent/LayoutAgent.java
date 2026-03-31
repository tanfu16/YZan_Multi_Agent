package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;

import java.util.List;

/**
 * 空间规划顾问
 * 从空间布局角度给方案建议
 */
public class LayoutAgent implements DecorationAgent{

    @Override
    public AgentResult execute(StructuredRequirement requirement){
        AgentResult result = new AgentResult();

        result.setAgentType(AgentType.LAYOUT);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(List.of("建议采用开放式客餐厅布局，提升空间通透感"));
        result.setRisks(List.of("如果收纳不足，开放布局会显得杂乱"));
        result.setSummary("布局方案偏向提升空间感和通透性");
        return result;
    }
}
