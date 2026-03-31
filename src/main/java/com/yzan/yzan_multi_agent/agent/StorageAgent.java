package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;

import java.util.List;

/**
 * 收纳优化顾问
 * 从收纳角度给方案建议
 */
public class StorageAgent implements DecorationAgent{

    @Override
    public AgentResult execute(StructuredRequirement requirement){
        AgentResult result = new AgentResult();

        result.setAgentType(AgentType.STORAGE);
        result.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        result.setRecommendations(List.of("建议增加储物柜，保证收纳空间充足"));
        result.setRisks(List.of("家具过多会导致收纳空间不足"));
        result.setSummary("需要有足够的储物柜保证收纳空间");

        return result;
    }
}
