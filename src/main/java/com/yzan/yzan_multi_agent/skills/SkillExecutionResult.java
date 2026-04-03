package com.yzan.yzan_multi_agent.skills;

import com.yzan.yzan_multi_agent.domain.FurnitureSearchResult;
import com.yzan.yzan_multi_agent.domain.MaterialStoreRecommendation;
import lombok.Data;

import java.util.List;

@Data
public class SkillExecutionResult {

    private boolean triggered;
    private String userRequest;
    private String skillName;
    private String skillPrompt;
    private String mcpService;
    private String message;
    private List<MaterialStoreRecommendation> materialStoreRecommendations;
    private List<FurnitureSearchResult> furnitureSearchResults;
}
