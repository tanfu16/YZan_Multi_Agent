package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeChunkRecordMapper {

    @Update("CREATE EXTENSION IF NOT EXISTS vector")
    void createVectorExtensionIfNotExists();

    @Update("""
            CREATE TABLE IF NOT EXISTS knowledge_chunk_record (
                id BIGSERIAL PRIMARY KEY,
                agent_domain VARCHAR(32) NOT NULL,
                source_name VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                content_hash VARCHAR(64) NOT NULL,
                embedding_json TEXT NOT NULL,
                embedding_vector VECTOR(1024),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP
            )
            """)
    void createTableIfNotExists();

    @Update("""
            ALTER TABLE knowledge_chunk_record
            ADD COLUMN IF NOT EXISTS embedding_vector VECTOR(1024)
            """)
    void addEmbeddingVectorColumnIfNotExists();

    @Update("""
            ALTER TABLE knowledge_chunk_record
            ADD COLUMN IF NOT EXISTS agent_domain VARCHAR(32)
            """)
    void addAgentDomainColumnIfNotExists();

    @Update("""
            UPDATE knowledge_chunk_record
            SET embedding_vector = embedding_json::vector
            WHERE embedding_vector IS NULL
              AND embedding_json IS NOT NULL
              AND embedding_json <> ''
            """)
    int backfillEmbeddingVectorFromJson();

    @Update("""
            UPDATE knowledge_chunk_record
            SET agent_domain = CASE
                WHEN source_name = 'RAG/child-safe-design.md' THEN 'SAFETY'
                WHEN source_name = 'RAG/elderly-safety.md' THEN 'SAFETY'
                WHEN source_name = 'RAG/anti-slip-and-flooring.md' THEN 'SAFETY'
                WHEN source_name = 'RAG/corner-and-cabinet-safety.md' THEN 'SAFETY'
                WHEN source_name = 'RAG/pet-friendly-materials.md' THEN 'SAFETY'
                ELSE 'SAFETY'
            END
            WHERE agent_domain IS NULL OR agent_domain = ''
            """)
    int backfillAgentDomainBySourceName();

    @Update("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_knowledge_chunk_record_content_hash
            ON knowledge_chunk_record(content_hash)
            """)
    void createContentHashIndexIfNotExists();

    @Update("""
            CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_source_name
            ON knowledge_chunk_record(source_name)
            """)
    void createSourceNameIndexIfNotExists();

    @Update("""
            CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_agent_domain
            ON knowledge_chunk_record(agent_domain)
            """)
    void createAgentDomainIndexIfNotExists();

    @Update("""
            CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_record_embedding_vector
            ON knowledge_chunk_record
            USING hnsw (embedding_vector vector_cosine_ops)
            """)
    void createEmbeddingVectorIndexIfNotExists();

    @Select("""
            SELECT COUNT(1)
            FROM knowledge_chunk_record
            """)
    long countAll();

    @Select("""
            SELECT
                id,
                agent_domain AS agentDomain,
                source_name AS sourceName,
                content,
                content_hash AS contentHash,
                embedding_json AS embeddingJson,
                embedding_vector::text AS embeddingVector,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM knowledge_chunk_record
            ORDER BY id
            """)
    List<KnowledgeChunkRecord> selectAll();

    @Select("""
            SELECT COUNT(1) > 0
            FROM knowledge_chunk_record
            WHERE agent_domain = #{agentDomain}
              AND source_name = #{sourceName}
              AND content = #{content}
            """)
    boolean existsByAgentDomainAndSourceNameAndContent(
            @Param("agentDomain") String agentDomain,
            @Param("sourceName") String sourceName,
            @Param("content") String content
    );

    @Select("""
            SELECT
                id,
                agent_domain AS agentDomain,
                source_name AS sourceName,
                content,
                content_hash AS contentHash,
                embedding_json AS embeddingJson,
                embedding_vector::text AS embeddingVector,
                (1 - (embedding_vector <=> #{queryVector}::vector)) AS similarityScore,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM knowledge_chunk_record
            WHERE embedding_vector IS NOT NULL
              AND agent_domain = #{agentDomain}
              AND (1 - (embedding_vector <=> #{queryVector}::vector)) >= #{minScore}
            ORDER BY embedding_vector <=> #{queryVector}::vector
            LIMIT #{maxResults}
            """)
    List<KnowledgeChunkRecord> searchSimilar(
            @Param("agentDomain") String agentDomain,
            @Param("queryVector") String queryVector,
            @Param("minScore") double minScore,
            @Param("maxResults") int maxResults
    );

    @Insert("""
            INSERT INTO knowledge_chunk_record (
                agent_domain,
                source_name,
                content,
                content_hash,
                embedding_json,
                embedding_vector,
                created_at,
                updated_at
            ) VALUES (
                #{agentDomain},
                #{sourceName},
                #{content},
                #{contentHash},
                #{embeddingJson},
                #{embeddingVector}::vector,
                #{createdAt},
                #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeChunkRecord record);
}
