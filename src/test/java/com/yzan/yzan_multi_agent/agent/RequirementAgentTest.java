package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
class RequirementAgentTest {

    @Autowired
    private RequirementAgent requirementAgent;

    @Test
    void shouldPrintStructuredRequirementFromFixedUserInput() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(89);
        userRequirement.setBudget(new BigDecimal("180000"));
        userRequirement.setFamilyMembers(List.of("夫妻"));
        userRequirement.setStylePreference("现代原木风");
        userRequirement.setSpecialNeeds(List.of("收纳多", "好打理"));
        userRequirement.setRawDescription("家里没有孩子，但是有一只猫，希望整体温馨、收纳充足、好清洁。");

        StructuredRequirement result = requirementAgent.execute(userRequirement);

        System.out.println("==== StructuredRequirement ====");
        System.out.println("houseType = " + result.getHouseType());
        System.out.println("area = " + result.getArea());
        System.out.println("budget = " + result.getBudget());
        System.out.println("familyProfile = " + result.getFamilyProfile());
        System.out.println("stylePreference = " + result.getStylePreference());
        System.out.println("priorities = " + result.getPriorities());
        System.out.println("constraints = " + result.getConstraints());
        System.out.println("===============================");
    }
}
