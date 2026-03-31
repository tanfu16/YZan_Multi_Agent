package com.yzan.yzan_multi_agent.domain;

import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import lombok.Data;

import java.util.List;

/**
 * agent返回结果
 */
@Data
public class AgentResult {

    private AgentType agentType; // 结果来自哪个agent

    private AgentExecutionStatus agentExecutionStatus; // agent执行结果

    private List<String> recommendations; // agent给出的核心建议列表

    private List<String> risks; // agent意识到的风险点

    private String summary; // agent的一句话总结
}
