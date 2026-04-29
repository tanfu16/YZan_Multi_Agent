package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.RequirementStateRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RequirementStateRecordMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS requirement_state_record (
                id BIGSERIAL PRIMARY KEY,
                session_id VARCHAR(64) NOT NULL UNIQUE,
                structured_requirement_json TEXT NOT NULL,
                version BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
            """)
    void createTableIfNotExists();

    @Update("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_requirement_state_record_session_id
            ON requirement_state_record(session_id)
            """)
    void createSessionIdIndexIfNotExists();

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                structured_requirement_json AS structuredRequirementJson,
                version,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM requirement_state_record
            WHERE session_id = #{sessionId}
            """)
    RequirementStateRecord selectBySessionId(String sessionId);

    @Insert("""
            INSERT INTO requirement_state_record (
                session_id,
                structured_requirement_json,
                version,
                created_at,
                updated_at
            ) VALUES (
                #{sessionId},
                #{structuredRequirementJson},
                #{version},
                #{createdAt},
                #{updatedAt}
            )
            ON CONFLICT (session_id) DO UPDATE
            SET structured_requirement_json = EXCLUDED.structured_requirement_json,
                version = requirement_state_record.version + 1,
                updated_at = EXCLUDED.updated_at
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(RequirementStateRecord record);
}
