package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeEmbeddingIndexer {

    private final PersistedKnowledgeChunkService persistedKnowledgeChunkService;

    public KnowledgeEmbeddingIndexer(PersistedKnowledgeChunkService persistedKnowledgeChunkService) {
        this.persistedKnowledgeChunkService = persistedKnowledgeChunkService;
    }

    @PostConstruct
    public void indexKnowledge() {
        persistedKnowledgeChunkService.ensureInitialized();
        List<KnowledgeChunkRecord> records = persistedKnowledgeChunkService.loadAllRecords();
        System.out.println("Knowledge chunks ready in pgvector = " + records.size());
    }
}
