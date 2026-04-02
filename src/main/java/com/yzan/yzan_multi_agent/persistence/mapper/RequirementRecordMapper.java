package com.yzan.yzan_multi_agent.persistence.mapper;

import com.yzan.yzan_multi_agent.persistence.record.RequirementRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface RequirementRecordMapper {

    @Insert("""
            INSERT INTO requirement_record (
                request_id,
                session_id,
                parent_request_id,
                user_id,
                user_requirement_json,
                structured_requirement_json,
                created_at,
                updated_at
            ) VALUES (
                #{requestId},
                #{sessionId},
                #{parentRequestId},
                #{userId},
                #{userRequirementJson},
                #{structuredRequirementJson},
                #{createdAt},
                #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RequirementRecord record);
}
