package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.IntentClassificationRequest;
import com.yzan.yzan_multi_agent.domain.IntentClassificationResult;

public interface UserIntentService {

    String PLAN_GENERATION = "PLAN_GENERATION";
    String PLAN_MODIFICATION = "PLAN_MODIFICATION";
    String MATERIAL_STORE_SKILL = "MATERIAL_STORE_SKILL";
    String FURNITURE_SEARCH_SKILL = "FURNITURE_SEARCH_SKILL";
    String GENERAL_CHAT = "GENERAL_CHAT";
    String CLARIFICATION = "CLARIFICATION";

    IntentClassificationResult classify(IntentClassificationRequest request);
}
