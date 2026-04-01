package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * 预算 Agent 处理后的结果
 */
@Data
public class BudgetAnalysisResult {

    private List<String> recommendations;
    private List<String> risks;
    private String summary;
}
