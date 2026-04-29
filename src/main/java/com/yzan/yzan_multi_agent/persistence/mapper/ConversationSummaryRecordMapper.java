package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.ConversationSummaryRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConversationSummaryRecordMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS conversation_summary_record (
                id BIGSERIAL PRIMARY KEY,
                session_id VARCHAR(64) NOT NULL,
                summary_text TEXT NOT NULL,
                start_turn INTEGER NOT NULL,
                end_turn INTEGER NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
            """)
    void createTableIfNotExists();

    @Update("""
            CREATE INDEX IF NOT EXISTS idx_conversation_summary_record_session_id
            ON conversation_summary_record(session_id)
            """)
    void createSessionIdIndexIfNotExists();

    @Update("""
            ALTER TABLE conversation_summary_record
            ADD COLUMN IF NOT EXISTS start_turn INTEGER
            """)
    void addStartTurnColumnIfNotExists();

    @Update("""
            ALTER TABLE conversation_summary_record
            ADD COLUMN IF NOT EXISTS end_turn INTEGER
            """)
    void addEndTurnColumnIfNotExists();

    @Update("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_name = 'conversation_summary_record'
                      AND column_name = 'turn_count_covered'
                ) THEN
                    UPDATE conversation_summary_record
                    SET start_turn = COALESCE(start_turn, 1),
                        end_turn = COALESCE(end_turn, turn_count_covered);
                END IF;
            END $$;
            """)
    void backfillTurnRangeFromCoveredTurns();

    @Update("""
            ALTER TABLE conversation_summary_record
            DROP COLUMN IF EXISTS turn_count_covered
            """)
    void dropTurnCountCoveredColumnIfExists();

    @Update("""
            DROP INDEX IF EXISTS uk_conversation_summary_record_session_id
            """)
    void dropLegacySessionUniqueIndexIfExists();

    @Update("""
            CREATE UNIQUE INDEX IF NOT EXISTS uk_conversation_summary_record_session_turn_range
            ON conversation_summary_record(session_id, start_turn, end_turn)
            """)
    void createTurnRangeIndexIfNotExists();

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                summary_text AS summaryText,
                start_turn AS startTurn,
                end_turn AS endTurn,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM conversation_summary_record
            WHERE session_id = #{sessionId}
            ORDER BY end_turn DESC, id DESC
            LIMIT 1
            """)
    ConversationSummaryRecord selectLatestBySessionId(String sessionId);

    @Select("""
            SELECT
                id,
                session_id AS sessionId,
                summary_text AS summaryText,
                start_turn AS startTurn,
                end_turn AS endTurn,
                created_at AS createdAt,
                updated_at AS updatedAt
            FROM conversation_summary_record
            WHERE session_id = #{sessionId}
            ORDER BY end_turn DESC, id DESC
            LIMIT #{limit}
            """)
    java.util.List<ConversationSummaryRecord> selectRecentBySessionId(String sessionId, int limit);

    @Insert("""
            INSERT INTO conversation_summary_record (
                session_id,
                summary_text,
                start_turn,
                end_turn,
                created_at,
                updated_at
            ) VALUES (
                #{sessionId},
                #{summaryText},
                #{startTurn},
                #{endTurn},
                #{createdAt},
                #{updatedAt}
            )
            ON CONFLICT (session_id, start_turn, end_turn) DO UPDATE
            SET summary_text = EXCLUDED.summary_text,
                updated_at = EXCLUDED.updated_at
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrUpdate(ConversationSummaryRecord record);
}
