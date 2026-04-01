package com.yzan.yzan_multi_agent.controller;


import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.workflow.DecorationWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 负责接受请求，并且调用DecorationWorkflowService
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private DecorationWorkflowService decorationWorkflowService;

    @PostMapping("/generate")
    public DecorationPlan generatePlan(@RequestBody UserRequirement userRequirement){
        return decorationWorkflowService.execute(userRequirement);
    }
}
