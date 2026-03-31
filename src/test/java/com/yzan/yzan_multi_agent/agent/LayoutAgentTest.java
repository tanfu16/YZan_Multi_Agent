package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LayoutAgentTest {

    @Test
    void shouldReturnSuccessfulLayoutResult(){

        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("180000"));
        requirement.setFamilyProfile("夫妻+孩子");
        requirement.setStylePreference("现代原木风");
        requirement.setPriorities(List.of("收纳", "安全", "通透"));
        requirement.setConstraints(List.of("避免复杂吊顶"));
        LayoutAgent agent = new LayoutAgent();
        AgentResult result = agent.execute(requirement);

        assertEquals(result.getAgentType(), AgentType.LAYOUT);
        assertEquals(result.getAgentExecutionStatus(), AgentExecutionStatus.SUCCESS);
    }

    @Test
    void shouldContainLayoutRecommendations(){

        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("180000"));
        requirement.setFamilyProfile("夫妻+孩子");
        requirement.setStylePreference("现代原木风");
        requirement.setPriorities(List.of("收纳", "安全", "通透"));
        requirement.setConstraints(List.of("避免复杂吊顶"));
        LayoutAgent agent = new LayoutAgent();
        AgentResult result = agent.execute(requirement);

        assertNotNull(result.getRecommendations());
        assertFalse(result.getRecommendations().isEmpty());

        assertNotNull(result.getSummary());
        assertFalse(result.getSummary().isBlank());

        String allText = String.join(" ", result.getRecommendations()) + " " + result.getSummary();

        assertTrue(
                allText.contains("布局")
                        || allText.contains("空间")
                        || allText.contains("通透")
                        || allText.contains("客餐厅")
                        || allText.contains("动线"),
                "LayoutAgent 的输出应该体现布局相关语义"
        );
    }

}