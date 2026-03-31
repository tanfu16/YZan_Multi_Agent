package com.yzan.yzan_multi_agent.workflow;

import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DecorationWorkflowServiceTest {


    @Test
    void shouldExecuteAllAgentsAndReturnResults(){

        DecorationWorkflowService service = new DecorationWorkflowService();

        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(200);
        userRequirement.setBudget(new BigDecimal("180000"));
        userRequirement.setFamilyMembers(List.of("夫妻", "儿子", "宠物"));
        userRequirement.setStylePreference("简约风");
        userRequirement.setSpecialNeeds(List.of("收纳多", "好打理"));
        userRequirement.setRawDescription("预算18万，装修一套200平方的房子，家里有夫妻和儿子，还有一只宠物，要求简约风，收纳多并且好打理");
        DecorationPlan results = service.execute(userRequirement);

        assertNotNull(results);

        assertNotNull(results.getSummary());
        assertFalse(results.getSummary().isBlank());

        assertNotNull(results.getKeyRecommendations());
        assertFalse(results.getKeyRecommendations().isEmpty());

        assertNotNull(results.getConflicts());
        assertFalse(results.getConflicts().isEmpty());

        assertNotNull(results.getFinalSuggestion());
        assertFalse(results.getFinalSuggestion().isBlank());

    }

}
