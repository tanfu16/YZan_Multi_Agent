package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationTurnRecord {

    private Long id;

    private String sessionId;

    private String rawInput;

    private String assistantOutput;

    private String intentType;

    private LocalDateTime createdAt;
}
