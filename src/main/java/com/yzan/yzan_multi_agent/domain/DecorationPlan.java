package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * 最终装修方案
 */
@Data
public class DecorationPlan {

    private String summary; // 整体方案摘要

    private List<ConflictItem> conflicts; // 冲突项

    private PlanOption primaryOption; // 主方案

    private List<PlanOption> alternativeOptions; // 备选方案

    private String decisionReason; // 为什么最终选择主方案
}
