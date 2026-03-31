package com.yzan.yzan_multi_agent.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * domain的类信息能否转成JSON
 */
class UserRequirementJSONTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUserRequirementToJson() throws Exception{
        UserRequirement requirement = new UserRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("1000000"));
        requirement.setFamilyMembers(List.of("夫妻","孩子","宠物"));
        requirement.setStylePreference("现代原木风");
        requirement.setSpecialNeeds(List.of("收纳多","好清洁"));
        requirement.setRawDescription("希望全屋温馨一点，预算不能超太多。");

        String json = objectMapper.writeValueAsString(requirement);

        assertTrue(json.contains("\"houseType\":\"两室一厅\""));
        assertTrue(json.contains("\"area\":89"));
        assertTrue(json.contains("\"stylePreference\":\"现代原木风\""));
    }
}