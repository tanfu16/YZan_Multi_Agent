package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanRecord {

    private Long id;

    private String requestId;

    private String sessionId;

    private String parentRequestId;

    private String userId;

    private String structuredRequirementJson;

    private String agentResultsJson;

    private String decorationPlanJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
