package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.knowledge.KnowledgeRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
class KeywordKnowledgeRetrievalServiceTest {

    @Autowired
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Test
    void shouldPrintRetrievedSafetyKnowledge() {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("三室两厅");
        requirement.setArea(118);
        requirement.setBudget(new BigDecimal("180000"));
        requirement.setFamilyProfile("夫妻+孩子+宠物");
        requirement.setStylePreference("现代简约");
        requirement.setPriorities(List.of("安全", "清洁", "收纳"));
        requirement.setConstraints(List.of("避免尖角", "重视防滑", "材料耐磨"));

        List<KnowledgeChunk> chunks = knowledgeRetrievalService.retrieveForSafety(requirement);

        System.out.println("========== Retrieved Knowledge Chunks ==========");
        System.out.println("Retrieved count = " + chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            System.out.println("Chunk " + (i + 1));
            System.out.println("Source = " + chunk.getSourceName());
            System.out.println("Content = ");
            System.out.println(chunk.getContent());
            System.out.println("----------------------------------------------");
        }
    }
}
