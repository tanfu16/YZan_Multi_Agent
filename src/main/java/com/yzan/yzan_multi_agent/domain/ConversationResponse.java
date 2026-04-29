package com.yzan.yzan_multi_agent.domain;

import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.skills.SkillExecutionResult;
import lombok.Data;

/**
 * 对前端统一返回会话处理结果。
 */
@Data
public class ConversationResponse {

    private String rawInput;

    private UserIntentType intentType;

    private String reply;

    private RequirementUnderstandingResult understanding;

    private DecorationPlan decorationPlan;

    private SkillExecutionResult skillExecutionResult;
}
