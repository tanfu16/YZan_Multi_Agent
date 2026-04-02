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
}
