package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;

import java.util.List;

public interface KnowledgeRetrievalService {

    List<KnowledgeChunk> retrieveForSafety(StructuredRequirement requirement);

    List<KnowledgeChunk> retrieveForBudget(StructuredRequirement requirement);

    List<KnowledgeChunk> retrieveForLayout(StructuredRequirement requirement);

    List<KnowledgeChunk> retrieveForStorage(StructuredRequirement requirement);
}