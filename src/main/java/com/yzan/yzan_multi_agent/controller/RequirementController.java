package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.agent.RequirementAgent;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementAgent requirementAgent;

    public RequirementController(RequirementAgent requirementAgent) {
        this.requirementAgent = requirementAgent;
    }

    @PostMapping("/structure")
    public StructuredRequirement structureRequirement(@RequestBody UserRequirement userRequirement) {
        return requirementAgent.execute(userRequirement);
    }
}
