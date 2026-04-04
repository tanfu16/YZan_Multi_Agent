package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeEmbeddingIndexer {

    private final PersistedKnowledgeChunkService persistedKnowledgeChunkService;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeEmbeddingIndexer(PersistedKnowledgeChunkService persistedKnowledgeChunkService,
                                     EmbeddingStore<TextSegment> embeddingStore) {
        this.persistedKnowledgeChunkService = persistedKnowledgeChunkService;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void indexKnowledge() {
        persistedKnowledgeChunkService.ensureInitialized();
        List<KnowledgeChunkRecord> records = persistedKnowledgeChunkService.loadAllRecords();
        System.out.println("Knowledge chunks loaded from database = " + records.size());

        for (KnowledgeChunkRecord record : records) {
            try {
                TextSegment segment = TextSegment.from(record.getContent());
                Embedding embedding = persistedKnowledgeChunkService.parseEmbedding(record);
                embeddingStore.add(embedding, segment);
                System.out.println("Indexed chunk from source = " + record.getSourceName());
            } catch (Exception e) {
                System.out.println("Failed to index chunk from source = "
                        + record.getSourceName() + ", error = " + e.getMessage());
            }
        }

        System.out.println("Knowledge indexing finished.");
    }
}
