package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RequirementStateRecord {

    private Long id;

    private String sessionId;

    private String structuredRequirementJson;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
