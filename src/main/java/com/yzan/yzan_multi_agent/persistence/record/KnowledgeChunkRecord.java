package com.yzan.yzan_multi_agent.persistence.record;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeChunkRecord {

    private Long id;
    private String sourceName;
    private String content;
    private String contentHash;
    private String embeddingJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
