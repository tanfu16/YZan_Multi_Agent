package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.agent.RequirementAgent;
import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementAgent requirementAgent;

    public RequirementController(RequirementAgent requirementAgent) {
        this.requirementAgent = requirementAgent;
    }

    @PostMapping("/understand")
    public RequirementUnderstandingResult understandRequirement(@RequestBody UserRequirement userRequirement) {
        return requirementAgent.understand(userRequirement);
    }
}
