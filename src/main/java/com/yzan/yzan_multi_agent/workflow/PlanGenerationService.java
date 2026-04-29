package com.yzan.yzan_multi_agent.workflow;

import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;

public interface PlanGenerationService {

    DecorationPlan execute(UserRequirement userRequirement);

    default DecorationPlan execute(UserRequirement userRequirement, StructuredRequirement structuredRequirement) {
        return execute(userRequirement);
    }
}
