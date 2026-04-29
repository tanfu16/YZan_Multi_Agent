package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.UserRequirement;

public interface RequirementUnderstandingService {

    RequirementUnderstandingResult understand(UserRequirement userRequirement);
}
