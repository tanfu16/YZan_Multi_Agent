package com.yzan.yzan_multi_agent.workflow;

import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DecorationWorkflowServiceTest {

    @Autowired
    private DecorationWorkflowService service;

    @Test
    void shouldExecuteAllAgentsAndReturnResults() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(200);
        userRequirement.setBudget(new BigDecimal("80000"));
        userRequirement.setFamilyMembers(List.of("夫妻", "女儿", "狗"));
        userRequirement.setStylePreference("简约风");
        userRequirement.setSpecialNeeds(List.of("清洁"));
        userRequirement.setRawDescription("预算8万，装修一套200平方米的房子，家里有夫妻、女儿和一只狗，要求好打理");

        DecorationPlan result = service.execute(userRequirement);

        assertNotNull(result);
        assertNotNull(result.getSummary());
        assertFalse(result.getSummary().isBlank());
        assertNotNull(result.getConflicts());
        assertNotNull(result.getPrimaryOption());
        assertNotNull(result.getPrimaryOption().getName());
        assertFalse(result.getPrimaryOption().getName().isBlank());
        assertNotNull(result.getPrimaryOption().getRecommendations());
        assertFalse(result.getPrimaryOption().getRecommendations().isEmpty());
        assertNotNull(result.getAlternativeOptions());
        assertFalse(result.getAlternativeOptions().isEmpty());
        assertNotNull(result.getDecisionReason());
        assertFalse(result.getDecisionReason().isBlank());
    }

    @Test
    void shouldPrintParallelAgentRagWorkflowResult() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("三室两厅");
        userRequirement.setArea(118);
        userRequirement.setBudget(new BigDecimal("180000"));
        userRequirement.setFamilyMembers(List.of("夫妻", "孩子", "宠物"));
        userRequirement.setStylePreference("现代简约");
        userRequirement.setSpecialNeeds(List.of("安全", "收纳", "好打理", "避免尖角", "重视防滑", "材料耐磨"));
        userRequirement.setRawDescription("预算18万，三室两厅，家里有夫妻、孩子和宠物，希望整体现代简约风，重点关注安全、收纳和日常清洁维护，避免尖角，重视防滑，并尽量选择耐磨材料。");

        DecorationPlan result = service.execute(userRequirement);

        System.out.println("========== Parallel Agent RAG Workflow Result ==========");
        System.out.println("UserRequirement = " + userRequirement);
        System.out.println("Summary = " + result.getSummary());
        System.out.println("Conflicts = " + result.getConflicts());
        System.out.println("PrimaryOption = " + result.getPrimaryOption());
        System.out.println("AlternativeOptions = " + result.getAlternativeOptions());
        System.out.println("DecisionReason = " + result.getDecisionReason());
        System.out.println("========================================================");
    }
}