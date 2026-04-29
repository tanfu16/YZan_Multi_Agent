package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.domain.IntentClassificationRequest;
import com.yzan.yzan_multi_agent.domain.IntentClassificationResult;
import com.yzan.yzan_multi_agent.service.UserIntentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intents")
public class IntentController {

    private final UserIntentService userIntentService;

    public IntentController(UserIntentService userIntentService) {
        this.userIntentService = userIntentService;
    }

    @PostMapping("/classify")
    public IntentClassificationResult classify(@RequestBody IntentClassificationRequest request) {
        return userIntentService.classify(request);
    }
}
