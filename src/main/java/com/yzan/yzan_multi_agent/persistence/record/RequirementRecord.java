package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RequirementRecord {

    private Long id;

    private String requestId;

    private String sessionId;

    private String parentRequestId;

    private String userId;

    private String userRequirementJson;

    private String structuredRequirementJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
