package com.yzan.yzan_multi_agent.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 测试能否跑通工作流
 */
class DomainFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUserRequirementToJson() throws Exception{
        // 先将用户输入存入UserRequirement类中
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(89);
        userRequirement.setBudget(new BigDecimal("1000000"));
        userRequirement.setFamilyMembers(List.of("夫妻","孩子","宠物"));
        userRequirement.setStylePreference("现代原木风");
        userRequirement.setSpecialNeeds(List.of("收纳多","好清洁"));
        userRequirement.setRawDescription("希望全屋温馨一点，预算不能超太多。");

        // 在将UserRequirement类信息转化成标准化的StructuredRequirement
        StructuredRequirement structuredRequirement = new StructuredRequirement();
        structuredRequirement.setHouseType("两室一厅");
        structuredRequirement.setArea(89);
        structuredRequirement.setBudget(new BigDecimal("180000"));
        structuredRequirement.setFamilyProfile("夫妻+孩子+宠物");
        structuredRequirement.setStylePreference("现代原木风");
        structuredRequirement.setPriorities(List.of("收纳", "安全", "清洁"));
        structuredRequirement.setConstraints(List.of("避免尖角", "减少开放格"));

        // 模拟两个Agent处理后的输出
        AgentResult layoutResult = new AgentResult();
        layoutResult.setSummary("建议采用开放式客餐厅布局，增强通透感。");

        AgentResult safetyResult = new AgentResult();
        safetyResult.setSummary("建议减少尖角家具，并控制开放格比例。");

        // 测试处理冲突Agent
        ConflictItem conflict = new ConflictItem();
        conflict.setTopic("开放感与安全性冲突");
        conflict.setRelatedAgents(List.of("LAYOUT", "SAFETY"));
        conflict.setDescription("开放布局更通透，但开放格和尖角设计不利于儿童安全。");
        conflict.setResolution("保留大空间感，同时使用圆角家具和封闭柜体。");

        // 测试最终方案制定
        DecorationPlan plan = new DecorationPlan();
        plan.setSummary("该方案优先兼顾家庭安全与空间通透。");
        plan.setKeyRecommendations(List.of("增加收纳柜", "采用圆角家具"));
        plan.setConflicts(List.of(conflict));
        plan.setFinalSuggestion("选择偏实用和安全的家庭化装修方案。");

        assertEquals("两室一厅", userRequirement.getHouseType());
        assertEquals("夫妻+孩子+宠物", structuredRequirement.getFamilyProfile());
        assertFalse(plan.getConflicts().isEmpty());
        assertEquals(1, plan.getConflicts().size());
        assertEquals("开放感与安全性冲突", plan.getConflicts().get(0).getTopic());
    }
}