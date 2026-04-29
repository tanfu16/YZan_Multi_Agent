package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class IntentClassificationResult {

    private String intent;

    private double confidence;

    private String reason;

    private boolean requirementModification;

    private String normalizedUserRequest;

    private String skillType;

    private String reply;
}
