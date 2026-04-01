package com.yzan.yzan_multi_agent.domain;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class CoordinatorAnalysisResult {

    private String summary;
    private List<String> keyRecommendations;
    private List<ConflictItem> conflicts;
    private String finalSuggestion;
}
