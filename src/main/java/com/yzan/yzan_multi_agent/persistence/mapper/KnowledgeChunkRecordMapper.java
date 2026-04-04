package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.KnowledgeChunkRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeChunkRecordMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS knowledge_chunk_record (
                id BIGSERIAL PRIMARY KEY,
                source_name VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                content_hash VARCHAR(64) NOT NULL,
                embedding_json TEXT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP
            )
            """)
    void createTableIfNotExists();

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

    @Select("""
            SELECT COUNT(1)
            FROM knowledge_chunk_record
            """)
    long countAll();

    @Select("""
            SELECT
                id,
                source_name AS sourceName,
                content,
                content_hash AS contentHash,
                embedding_json AS embeddingJson,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM knowledge_chunk_record
            ORDER BY id
            """)
    List<KnowledgeChunkRecord> selectAll();

    @Insert("""
            INSERT INTO knowledge_chunk_record (
                source_name,
                content,
                content_hash,
                embedding_json,
                created_at,
                updated_at
            ) VALUES (
                #{sourceName},
                #{content},
                #{contentHash},
                #{embeddingJson},
                #{createdAt},
                #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeChunkRecord record);
}
