package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BudgetAgentTest {

    @Autowired
    private BudgetAgent budgetAgent;

    @Test
    void shouldPrintBudgetAgentResultFromFixedRequirement() {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("180000"));
        requirement.setFamilyProfile("夫妻+宠物");
        requirement.setStylePreference("现代原木风");
        requirement.setPriorities(List.of("收纳", "清洁", "通透"));
        requirement.setConstraints(List.of("选择耐脏耐磨材料", "避免复杂吊顶"));

        AgentResult result = budgetAgent.execute(requirement);

        System.out.println("==== BudgetAgent Result ====");
        System.out.println("agentType = " + result.getAgentType());
        System.out.println("status = " + result.getAgentExecutionStatus());
        System.out.println("recommendations = " + result.getRecommendations());
        System.out.println("risks = " + result.getRisks());
        System.out.println("summary = " + result.getSummary());
        System.out.println("============================");

        assertNotNull(result);
        assertNotNull(result.getRecommendations());
        assertFalse(result.getRecommendations().isEmpty());
        assertNotNull(result.getSummary());
        assertFalse(result.getSummary().isBlank());
    }
}
