package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class IntentClassificationRequest {

    private String sessionId;

    private String userRequest;
}
