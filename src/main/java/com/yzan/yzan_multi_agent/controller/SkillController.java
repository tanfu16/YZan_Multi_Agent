package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.domain.SkillExecutionRequest;
import com.yzan.yzan_multi_agent.skills.SkillExecutionResult;
import com.yzan.yzan_multi_agent.skills.SkillExecutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillExecutionService skillExecutionService;

    public SkillController(SkillExecutionService skillExecutionService) {
        this.skillExecutionService = skillExecutionService;
    }

    @PostMapping("/execute")
    public SkillExecutionResult execute(@RequestBody SkillExecutionRequest request) {
        return skillExecutionService.execute(
                request.getUserRequest(),
                request.getLocation(),
                request.getMaterialKeyword(),
                request.getPlatform(),
                request.getFurnitureKeyword()
        );
    }
}
