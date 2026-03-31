package com.yzan.yzan_multi_agent.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试domain类信息的存储
 */
class UserRequirementTest {

    @Test
    void shouldCreateUserRequirementSuccessfully(){
        UserRequirement requirement = new UserRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("1000000"));
        requirement.setFamilyMembers(List.of("夫妻","孩子","宠物"));
        requirement.setStylePreference("现代原木风");
        requirement.setSpecialNeeds(List.of("收纳多","好清洁"));
        requirement.setRawDescription("希望全屋温馨一点，预算不能超太多。");

        assertEquals("两室一厅", requirement.getHouseType());
        assertEquals(89, requirement.getArea());
        assertEquals(new BigDecimal("1000000"), requirement.getBudget());
        assertEquals(3, requirement.getFamilyMembers().size());
        assertEquals("现代原木风", requirement.getStylePreference());
        assertEquals(2, requirement.getSpecialNeeds().size());
        assertEquals("希望全屋温馨一点，预算不能超太多。", requirement.getRawDescription());
    }
}