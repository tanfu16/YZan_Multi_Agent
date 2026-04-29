package com.yzan.yzan_multi_agent.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.persistence.mapper.KnowledgeChunkRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistedKnowledgeChunkServiceTest {

    @Test
    void shouldPassAgentDomainIntoVectorSearch() {
        CapturingKnowledgeChunkRecordMapper mapper = new CapturingKnowledgeChunkRecordMapper();
        PersistedKnowledgeChunkService service = new PersistedKnowledgeChunkService(
                new EmptyKnowledgeLoader(),
                mapper,
                null,
                new ObjectMapper()
        );

        service.searchSimilar("BUDGET", List.of(0.1f, 0.2f), 0.5, 5);

        assertEquals("BUDGET", mapper.lastAgentDomain);
        assertEquals("[0.1,0.2]", mapper.lastQueryVector);
        assertEquals(0.5, mapper.lastMinScore);
        assertEquals(5, mapper.lastMaxResults);
    }

    private static final class EmptyKnowledgeLoader implements KnowledgeLoader {
        @Override
        public List<com.yzan.yzan_multi_agent.domain.KnowledgeChunk> loadAllChunks() {
            return List.of();
        }
    }

    private static final class CapturingKnowledgeChunkRecordMapper implements KnowledgeChunkRecordMapper {
        private String lastAgentDomain;
        private String lastQueryVector;
        private double lastMinScore;
        private int lastMaxResults;

        @Override
        public List<KnowledgeChunkRecord> searchSimilar(String agentDomain, String queryVector, double minScore, int maxResults) {
            this.lastAgentDomain = agentDomain;
            this.lastQueryVector = queryVector;
            this.lastMinScore = minScore;
            this.lastMaxResults = maxResults;
            return List.of();
        }

        @Override public void createVectorExtensionIfNotExists() {}
        @Override public void createTableIfNotExists() {}
        @Override public void addEmbeddingVectorColumnIfNotExists() {}
        @Override public void addAgentDomainColumnIfNotExists() {}
        @Override public int backfillEmbeddingVectorFromJson() { return 0; }
        @Override public int backfillAgentDomainBySourceName() { return 0; }
        @Override public void createContentHashIndexIfNotExists() {}
        @Override public void createSourceNameIndexIfNotExists() {}
        @Override public void createAgentDomainIndexIfNotExists() {}
        @Override public void createEmbeddingVectorIndexIfNotExists() {}
        @Override public long countAll() { return 0; }
        @Override public List<KnowledgeChunkRecord> selectAll() { return List.of(); }
        @Override public boolean existsByAgentDomainAndSourceNameAndContent(String agentDomain, String sourceName, String content) { return false; }
        @Override public int insert(KnowledgeChunkRecord record) { return 0; }
    }
}
