package com.yzan.yzan_multi_agent.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 测试领域对象能否串起一条完整数据流
 */
class DomainFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUserRequirementToJson() throws Exception {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(89);
        userRequirement.setBudget(new BigDecimal("1000000"));
        userRequirement.setFamilyMembers(List.of("夫妻", "孩子", "宠物"));
        userRequirement.setStylePreference("现代原木风");
        userRequirement.setSpecialNeeds(List.of("收纳多", "好清洁"));
        userRequirement.setRawDescription("希望全屋温馨一点，预算不能超太多。");

        StructuredRequirement structuredRequirement = new StructuredRequirement();
        structuredRequirement.setHouseType("两室一厅");
        structuredRequirement.setArea(89);
        structuredRequirement.setBudget(new BigDecimal("180000"));
        structuredRequirement.setFamilyProfile("夫妻+孩子+宠物");
        structuredRequirement.setStylePreference("现代原木风");
        structuredRequirement.setPriorities(List.of("收纳", "安全", "清洁"));
        structuredRequirement.setConstraints(List.of("避免尖角", "减少开放格"));

        AgentResult layoutResult = new AgentResult();
        layoutResult.setSummary("建议采用开放式客餐厅布局，增强通透感。");

        AgentResult safetyResult = new AgentResult();
        safetyResult.setSummary("建议减少尖角家具，并控制开放格比例。");

        ConflictItem conflict = new ConflictItem();
        conflict.setTopic("开放感与安全性冲突");
        conflict.setRelatedAgents(List.of("LAYOUT", "SAFETY"));
        conflict.setDescription("开放式布局更通透，但开放格和尖角设计不利于儿童安全。");
        conflict.setResolution("保留大空间感，同时使用圆角家具和封闭柜体。");

        PlanOption primaryOption = new PlanOption();
        primaryOption.setName("平衡型主方案");
        primaryOption.setPositioning("优先兼顾家庭安全、收纳和空间通透感");
        primaryOption.setRecommendations(List.of("增加收纳柜", "采用圆角家具"));
        primaryOption.setAdvantages(List.of("兼顾安全和日常实用性"));
        primaryOption.setDisadvantages(List.of("开放感不如极简方案强"));
        primaryOption.setApplicableCrowd("适合有孩子或宠物的家庭");

        PlanOption alternativeOption = new PlanOption();
        alternativeOption.setName("通透优先方案");
        alternativeOption.setPositioning("优先提升公共区域的开放感");
        alternativeOption.setRecommendations(List.of("减少高柜体数量", "增加开放式布局比例"));
        alternativeOption.setAdvantages(List.of("视觉更轻盈，空间感更强"));
        alternativeOption.setDisadvantages(List.of("安全性和收纳能力会有所让步"));
        alternativeOption.setApplicableCrowd("适合更重视空间体验的家庭");

        DecorationPlan plan = new DecorationPlan();
        plan.setSummary("该方案优先兼顾家庭安全与空间通透。");
        plan.setConflicts(List.of(conflict));
        plan.setPrimaryOption(primaryOption);
        plan.setAlternativeOptions(List.of(alternativeOption));
        plan.setDecisionReason("主方案更适合当前家庭结构，能在安全、收纳和通透感之间取得平衡。");

        String userRequirementJson = objectMapper.writeValueAsString(userRequirement);
        String decorationPlanJson = objectMapper.writeValueAsString(plan);

        assertNotNull(userRequirementJson);
        assertNotNull(decorationPlanJson);

        assertEquals("两室一厅", userRequirement.getHouseType());
        assertEquals("夫妻+孩子+宠物", structuredRequirement.getFamilyProfile());

        assertFalse(plan.getConflicts().isEmpty());
        assertEquals(1, plan.getConflicts().size());
        assertEquals("开放感与安全性冲突", plan.getConflicts().get(0).getTopic());

        assertNotNull(plan.getPrimaryOption());
        assertEquals("平衡型主方案", plan.getPrimaryOption().getName());
        assertFalse(plan.getPrimaryOption().getRecommendations().isEmpty());

        assertNotNull(plan.getAlternativeOptions());
        assertEquals(1, plan.getAlternativeOptions().size());

        assertNotNull(plan.getDecisionReason());
        assertFalse(plan.getDecisionReason().isBlank());
    }
}
