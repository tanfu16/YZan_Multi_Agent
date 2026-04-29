package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.domain.ConversationResponse;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.service.ConversationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/handle")
    public ConversationResponse handle(@RequestBody UserRequirement userRequirement) {
        return conversationService.handle(userRequirement);
    }
}
