package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.AgentExecutionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface AgentExecutionRecordMapper {

    @Insert("""
            INSERT INTO agent_execution_record (
                request_id,
                session_id,
                parent_request_id,
                user_id,
                structured_requirement_json,
                agent_result_json,
                created_at,
                updated_at
            ) VALUES (
                #{requestId},
                #{sessionId},
                #{parentRequestId},
                #{userId},
                #{structuredRequirementJson},
                #{agentResultJson},
                #{createdAt},
                #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentExecutionRecord record);
}
