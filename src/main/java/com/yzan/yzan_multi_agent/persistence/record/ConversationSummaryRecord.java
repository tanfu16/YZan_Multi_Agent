package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationSummaryRecord {

    private Long id;

    private String sessionId;

    private String summaryText;

    private Integer startTurn;

    private Integer endTurn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
