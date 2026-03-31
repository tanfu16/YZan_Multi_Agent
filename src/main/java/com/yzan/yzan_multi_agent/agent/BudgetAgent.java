package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;

import java.util.List;

/**
 * 预算控制顾问
 * 从预算和成本控制角度给建议
 */
public class BudgetAgent implements DecorationAgent{

    @Override
    public AgentResult execute(StructuredRequirement requirement){
        AgentResult result = new AgentResult();

        result.setAgentType(AgentType.BUDGET);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(List.of("建议不要采用高级家具，保证预算"));
        result.setRisks(List.of("如果都采用品牌，预算会不足"));
        result.setSummary("适当降低品牌要求以保证预算");
        return result;
    }
}
