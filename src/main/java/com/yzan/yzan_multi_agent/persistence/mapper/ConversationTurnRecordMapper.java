package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.ConversationTurnRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ConversationTurnRecordMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS conversation_turn_record (
                id BIGSERIAL PRIMARY KEY,
                session_id VARCHAR(64) NOT NULL,
                raw_input TEXT NOT NULL,
                assistant_output TEXT,
                intent_type VARCHAR(64),
                created_at TIMESTAMP NOT NULL
            )
            """)
    void createTableIfNotExists();

    @Update("""
            CREATE INDEX IF NOT EXISTS idx_conversation_turn_record_session_id
            ON conversation_turn_record(session_id)
            """)
    void createSessionIdIndexIfNotExists();

    @Insert("""
            INSERT INTO conversation_turn_record (
                session_id,
                raw_input,
                assistant_output,
                intent_type,
                created_at
            ) VALUES (
                #{sessionId},
                #{rawInput},
                #{assistantOutput},
                #{intentType},
                #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConversationTurnRecord record);

    @Select("""
            SELECT COUNT(1)
            FROM conversation_turn_record
            WHERE session_id = #{sessionId}
            """)
    int countBySessionId(String sessionId);

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                raw_input AS rawInput,
                assistant_output AS assistantOutput,
                intent_type AS intentType,
                created_at AS createdAt
            FROM conversation_turn_record
            WHERE session_id = #{sessionId}
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<ConversationTurnRecord> selectRecentBySessionId(String sessionId, int limit);

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                raw_input AS rawInput,
                assistant_output AS assistantOutput,
                intent_type AS intentType,
                created_at AS createdAt
            FROM conversation_turn_record
            WHERE session_id = #{sessionId}
              AND id > #{afterId}
            ORDER BY id ASC
            LIMIT #{limit}
            """)
    List<ConversationTurnRecord> selectBySessionIdAfterId(@Param("sessionId") String sessionId,
                                                          @Param("afterId") long afterId,
                                                          @Param("limit") int limit);
}
