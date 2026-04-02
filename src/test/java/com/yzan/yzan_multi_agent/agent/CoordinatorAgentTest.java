package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import com.yzan.yzan_multi_agent.domain.enums.ConflictSeverity;
import com.yzan.yzan_multi_agent.domain.enums.ConflictType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CoordinatorAgentTest {

    @Autowired
    private CoordinatorAgent coordinatorAgent;

    @Test
    void shouldBuildBudgetPrimaryPlanAndDistinctAlternativeOptions() {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("80000"));
        requirement.setFamilyProfile("夫妻+宠物");
        requirement.setStylePreference("现代原木风");
        requirement.setPriorities(List.of("预算", "收纳", "清洁"));
        requirement.setConstraints(List.of("选择耐脏耐磨材料", "避免复杂吊顶"));

        AgentResult layoutResult = new AgentResult();
        layoutResult.setAgentType(AgentType.LAYOUT);
        layoutResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        layoutResult.setRecommendations(List.of(
                "建议采用开放式客餐厅布局，增强空间通透感",
                "玄关区域增加收纳柜体，提升空间利用率"
        ));
        layoutResult.setRisks(List.of("开放式布局对日常整洁要求较高"));
        layoutResult.setSummary("布局建议偏向提升空间感与功能分区合理性");

        AgentResult budgetResult = new AgentResult();
        budgetResult.setAgentType(AgentType.BUDGET);
        budgetResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        budgetResult.setRecommendations(List.of(
                "建议优先保障核心功能区预算投入",
                "减少非必要装饰性材料开支"
        ));
        budgetResult.setRisks(List.of("同时追求高颜值和高收纳可能导致预算超支"));
        budgetResult.setSummary("预算建议偏向控制风险并优先保证实用性");

        AgentResult safetyResult = new AgentResult();
        safetyResult.setAgentType(AgentType.SAFETY);
        safetyResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        safetyResult.setRecommendations(List.of(
                "建议采用圆角家具，降低宠物活动时的磕碰风险",
                "建议选择防滑和耐磨材料，提升长期使用安全性"
        ));
        safetyResult.setRisks(List.of("尖角家具和不耐磨材料可能带来安全和维护风险"));
        safetyResult.setSummary("安全建议偏向家庭适配和日常使用安全");

        AgentResult storageResult = new AgentResult();
        storageResult.setAgentType(AgentType.STORAGE);
        storageResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        storageResult.setRecommendations(List.of(
                "建议增加玄关和客厅储物柜，提升收纳能力",
                "优先使用封闭式柜体，减少积灰问题"
        ));
        storageResult.setRisks(List.of("柜体过多可能压缩空间感"));
        storageResult.setSummary("收纳建议偏向储物能力与空间平衡");

        List<AgentResult> results = List.of(
                layoutResult,
                budgetResult,
                safetyResult,
                storageResult
        );

        DecorationPlan plan = coordinatorAgent.execute(requirement, results);

        assertNotNull(plan);
        assertNotNull(plan.getSummary());
        assertFalse(plan.getSummary().isBlank());

        assertNotNull(plan.getConflicts());
        assertFalse(plan.getConflicts().isEmpty());

        assertTrue(
                plan.getConflicts().stream().anyMatch(conflict ->
                        conflict.getConflictType() == ConflictType.BUDGET_VS_STORAGE
                                && conflict.getSeverity() == ConflictSeverity.HIGH
                )
        );

        assertNotNull(plan.getPrimaryOption());
        assertEquals("预算优先主方案", plan.getPrimaryOption().getName());
        assertNotNull(plan.getPrimaryOption().getRecommendations());
        assertFalse(plan.getPrimaryOption().getRecommendations().isEmpty());

        assertNotNull(plan.getAlternativeOptions());
        assertFalse(plan.getAlternativeOptions().isEmpty());

        boolean hasSameOptionAsPrimary = plan.getAlternativeOptions().stream()
                .anyMatch(option -> option.getName().equals(plan.getPrimaryOption().getName()));
        assertFalse(hasSameOptionAsPrimary);

        assertTrue(
                plan.getAlternativeOptions().stream()
                        .allMatch(option -> option.getName() != null && !option.getName().isBlank())
        );

        assertNotNull(plan.getDecisionReason());
        assertFalse(plan.getDecisionReason().isBlank());
    }

    @Test
    void shouldBuildSafetyPrimaryPlanWhenSafetyIsPriority() {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("三室两厅");
        requirement.setArea(120);
        requirement.setBudget(new BigDecimal("200000"));
        requirement.setFamilyProfile("夫妻+孩子+宠物");
        requirement.setStylePreference("简约风");
        requirement.setPriorities(List.of("安全", "收纳"));
        requirement.setConstraints(List.of("避免尖角家具", "重视防滑"));

        AgentResult layoutResult = new AgentResult();
        layoutResult.setAgentType(AgentType.LAYOUT);
        layoutResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        layoutResult.setRecommendations(List.of("建议采用开放式公共区域布局"));
        layoutResult.setRisks(List.of("开放式布局会提高活动碰撞风险"));
        layoutResult.setSummary("布局建议偏向通透感");

        AgentResult budgetResult = new AgentResult();
        budgetResult.setAgentType(AgentType.BUDGET);
        budgetResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        budgetResult.setRecommendations(List.of("预算可以覆盖核心安全材料升级"));
        budgetResult.setRisks(List.of("局部升级会增加部分支出"));
        budgetResult.setSummary("预算基本可支撑主要诉求");

        AgentResult safetyResult = new AgentResult();
        safetyResult.setAgentType(AgentType.SAFETY);
        safetyResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        safetyResult.setRecommendations(List.of(
                "建议采用圆角家具",
                "建议选择防滑材料"
        ));
        safetyResult.setRisks(List.of("尖角和湿滑地面会提高儿童与宠物活动风险"));
        safetyResult.setSummary("安全建议优先级高");

        AgentResult storageResult = new AgentResult();
        storageResult.setAgentType(AgentType.STORAGE);
        storageResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        storageResult.setRecommendations(List.of("建议增加封闭式低位收纳"));
        storageResult.setRisks(List.of("柜体增加可能影响部分空间通透感"));
        storageResult.setSummary("收纳建议偏向安全和整洁兼顾");

        DecorationPlan plan = coordinatorAgent.execute(
                requirement,
                List.of(layoutResult, budgetResult, safetyResult, storageResult)
        );

        assertNotNull(plan);
        assertNotNull(plan.getPrimaryOption());
        assertEquals("安全优先主方案", plan.getPrimaryOption().getName());

        assertTrue(
                plan.getConflicts().stream().anyMatch(conflict ->
                        conflict.getConflictType() == ConflictType.LAYOUT_VS_SAFETY
                                && conflict.getSeverity() == ConflictSeverity.HIGH
                                && "PRIORITIZE_SAFETY".equals(conflict.getChosenDirection())
                )
        );

        boolean hasSameOptionAsPrimary = plan.getAlternativeOptions().stream()
                .anyMatch(option -> option.getName().equals(plan.getPrimaryOption().getName()));
        assertFalse(hasSameOptionAsPrimary);
    }
}
