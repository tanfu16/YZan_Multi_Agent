package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;

public interface DecorationAgent {
    AgentResult execute(StructuredRequirement requirement);
}
