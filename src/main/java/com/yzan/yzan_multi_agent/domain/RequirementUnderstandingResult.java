package com.yzan.yzan_multi_agent.domain;

import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import lombok.Data;

/**
 * RequirementAgent 对用户请求的统一理解结果。
 */
@Data
public class RequirementUnderstandingResult {

    private String rawInput;

    private StructuredRequirement structuredRequirement;

    private StructuredRequirementPatch structuredRequirementPatch;

    private UserIntentType intentType;

    private String reply;

    private String skillName;
}
