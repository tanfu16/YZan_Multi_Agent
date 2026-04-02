package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.PlanRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface PlanRecordMapper {

    @Insert("""
            INSERT INTO plan_record (
                request_id,
                session_id,
                parent_request_id,
                user_id,
                structured_requirement_json,
                agent_results_json,
                decoration_plan_json,
                created_at,
                updated_at
            ) VALUES (
                #{requestId},
                #{sessionId},
                #{parentRequestId},
                #{userId},
                #{structuredRequirementJson},
                #{agentResultsJson},
                #{decorationPlanJson},
                #{createdAt},
                #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlanRecord record);
}
