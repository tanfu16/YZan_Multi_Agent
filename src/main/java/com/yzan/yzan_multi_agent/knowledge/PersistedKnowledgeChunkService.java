package com.yzan.yzan_multi_agent.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.persistence.mapper.KnowledgeChunkRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PersistedKnowledgeChunkService {

    private final KnowledgeLoader knowledgeLoader;
    private final KnowledgeChunkRecordMapper knowledgeChunkRecordMapper;
    private final QwenEmbeddingModel qwenEmbeddingModel;
    private final ObjectMapper objectMapper;

    private volatile List<KnowledgeChunkRecord> cachedRecords = List.of();
    private final Map<String, String> sourceNameCache = new ConcurrentHashMap<>();

    public PersistedKnowledgeChunkService(KnowledgeLoader knowledgeLoader,
                                          KnowledgeChunkRecordMapper knowledgeChunkRecordMapper,
                                          QwenEmbeddingModel qwenEmbeddingModel,
                                          ObjectMapper objectMapper) {
        this.knowledgeLoader = knowledgeLoader;
        this.knowledgeChunkRecordMapper = knowledgeChunkRecordMapper;
        this.qwenEmbeddingModel = qwenEmbeddingModel;
        this.objectMapper = objectMapper;
    }

    public synchronized void ensureInitialized() {
        ensureTableReady();

        if (knowledgeChunkRecordMapper.countAll() > 0) {
            reloadCache();
            return;
        }

        List<KnowledgeChunk> chunks = knowledgeLoader.loadAllChunks();
        System.out.println("Persisting knowledge chunks to database = " + chunks.size());

        for (KnowledgeChunk chunk : chunks) {
            try {
                Embedding embedding = qwenEmbeddingModel.embed(chunk.getContent()).content();

                KnowledgeChunkRecord record = new KnowledgeChunkRecord();
                record.setSourceName(chunk.getSourceName());
                record.setContent(chunk.getContent());
                record.setContentHash(buildContentHash(chunk));
                record.setEmbeddingJson(objectMapper.writeValueAsString(embedding.vectorAsList()));
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                knowledgeChunkRecordMapper.insert(record);
            } catch (Exception e) {
                System.out.println("Failed to persist knowledge chunk from source = "
                        + chunk.getSourceName() + ", error = " + e.getMessage());
            }
        }

        reloadCache();
    }

    private void ensureTableReady() {
        try {
            knowledgeChunkRecordMapper.countAll();
        } catch (BadSqlGrammarException e) {
            System.out.println("knowledge_chunk_record table not found. Creating it automatically...");
            knowledgeChunkRecordMapper.createTableIfNotExists();
            knowledgeChunkRecordMapper.createContentHashIndexIfNotExists();
            knowledgeChunkRecordMapper.createSourceNameIndexIfNotExists();
        }
    }

    public List<KnowledgeChunkRecord> loadAllRecords() {
        if (cachedRecords.isEmpty()) {
            reloadCache();
        }
        return cachedRecords;
    }

    public String resolveSourceName(String content) {
        if (content == null || content.isBlank()) {
            return "vector-store";
        }

        if (sourceNameCache.isEmpty()) {
            rebuildSourceNameCache(loadAllRecords());
        }

        return sourceNameCache.getOrDefault(content, "vector-store");
    }

    public Embedding parseEmbedding(KnowledgeChunkRecord record) {
        try {
            List<Float> vector = objectMapper.readValue(record.getEmbeddingJson(), new TypeReference<>() {
            });
            return Embedding.from(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse persisted embedding for source: " + record.getSourceName(), e);
        }
    }

    private void reloadCache() {
        List<KnowledgeChunkRecord> records = knowledgeChunkRecordMapper.selectAll();
        cachedRecords = records;
        rebuildSourceNameCache(records);
    }

    private void rebuildSourceNameCache(List<KnowledgeChunkRecord> records) {
        sourceNameCache.clear();
        for (KnowledgeChunkRecord record : records) {
            sourceNameCache.put(record.getContent(), record.getSourceName());
        }
    }

    private String buildContentHash(KnowledgeChunk chunk) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((chunk.getSourceName() + "::" + chunk.getContent())
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build knowledge content hash.", e);
        }
    }
}
