package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeEmbeddingIndexer {

    private final KnowledgeLoader knowledgeLoader;
    private final QwenEmbeddingModel qwenEmbeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeEmbeddingIndexer(KnowledgeLoader knowledgeLoader,
                                     QwenEmbeddingModel qwenEmbeddingModel,
                                     EmbeddingStore<TextSegment> embeddingStore) {
        this.knowledgeLoader = knowledgeLoader;
        this.qwenEmbeddingModel = qwenEmbeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void indexKnowledge() {
        List<KnowledgeChunk> chunks = knowledgeLoader.loadAllChunks();
        System.out.println("Knowledge chunks to index = " + chunks.size());

        for (KnowledgeChunk chunk : chunks) {
            try {
                TextSegment segment = TextSegment.from(chunk.getContent());
                Embedding embedding = qwenEmbeddingModel.embed(segment.text()).content();
                embeddingStore.add(embedding, segment);

                System.out.println("Indexed chunk from source = " + chunk.getSourceName());
            } catch (Exception e) {
                System.out.println("Failed to index chunk from source = "
                        + chunk.getSourceName() + ", error = " + e.getMessage());
            }
        }

        System.out.println("Knowledge indexing finished.");
    }
}
