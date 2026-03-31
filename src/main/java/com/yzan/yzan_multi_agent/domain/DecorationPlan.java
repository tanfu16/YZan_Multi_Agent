package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * 方案
 */
@Data
public class DecorationPlan {

    private String summary; // 整体摘要

    private List<String> keyRecommendations; // 最终关键建议

    private List<ConflictItem> conflicts; // 冲突项

    private String finalSuggestion; // 最后推荐结论
}
