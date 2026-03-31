package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;

import java.util.List;

/**
 * 家庭安全顾问
 * 从安全性和家庭适配性角度给建议
 */
public class SafetyAgent implements DecorationAgent{

    @Override
    public AgentResult execute(StructuredRequirement requirement){
        AgentResult result = new AgentResult();

        result.setAgentType(AgentType.SAFETY);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(List.of("建议采用圆角家具，保证孩子安全"));
        result.setRisks(List.of("如果家具的角很锋利，会导致孩子受伤"));
        result.setSummary("采用圆角保证孩子安全");
        return result;
    }
}
