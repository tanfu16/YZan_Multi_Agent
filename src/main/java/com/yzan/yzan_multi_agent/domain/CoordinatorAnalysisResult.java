package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

@Data
public class CoordinatorAnalysisResult {

    private String summary;

    private List<ConflictItem> conflicts;

    private PlanOption primaryOption;

    private List<PlanOption> alternativeOptions;

    private String decisionReason;
}
