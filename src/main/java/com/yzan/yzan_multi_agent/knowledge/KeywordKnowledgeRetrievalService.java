package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;

import java.util.List;

public class KeywordKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final KnowledgeLoader knowledgeLoader;

    public KeywordKnowledgeRetrievalService(KnowledgeLoader knowledgeLoader) {
        this.knowledgeLoader = knowledgeLoader;
    }

    @Override
    public List<KnowledgeChunk> retrieveForSafety(StructuredRequirement requirement) {
        List<KnowledgeChunk> allChunks = knowledgeLoader.loadAllChunks();
        System.out.println("Loaded knowledge chunks count = " + allChunks.size());
        return List.of();
    }

    @Override
    public List<KnowledgeChunk> retrieveForBudget(StructuredRequirement requirement) {
        return List.of();
    }

    @Override
    public List<KnowledgeChunk> retrieveForLayout(StructuredRequirement requirement) {
        return List.of();
    }

    @Override
    public List<KnowledgeChunk> retrieveForStorage(StructuredRequirement requirement) {
        return List.of();
    }
}