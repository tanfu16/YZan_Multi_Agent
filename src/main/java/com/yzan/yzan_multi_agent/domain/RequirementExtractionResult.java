package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * LLM 处理后的对象
 */
@Data
public class RequirementExtractionResult {

    private String familyProfile;
    private String stylePreference;
    private List<String> priorities;
    private List<String> constraints;
}
