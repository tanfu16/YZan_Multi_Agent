package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CoordinatorAgentTest {

    @Autowired
    private CoordinatorAgent coordinatorAgent;

    @Test
    void shouldPrintDecorationPlanFromFixedAgentResults() {
        StructuredRequirement requirement = new StructuredRequirement();
        requirement.setHouseType("两室一厅");
        requirement.setArea(89);
        requirement.setBudget(new BigDecimal("180000"));
        requirement.setFamilyProfile("夫妻+宠物");
        requirement.setStylePreference("现代原木风");
        requirement.setPriorities(List.of("收纳", "清洁", "通透"));
        requirement.setConstraints(List.of("选择耐脏耐磨材料", "避免复杂吊顶"));

        AgentResult layoutResult = new AgentResult();
        layoutResult.setAgentType(AgentType.LAYOUT);
        layoutResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        layoutResult.setRecommendations(List.of(
                "建议采用开放式客餐厅布局，增强通透感",
                "玄关区域增加收纳柜体，提升空间利用率"
        ));
        layoutResult.setRisks(List.of(
                "开放式布局对日常整洁要求较高"
        ));
        layoutResult.setSummary("布局建议偏向提升空间感与功能分区合理性");

        AgentResult budgetResult = new AgentResult();
        budgetResult.setAgentType(AgentType.BUDGET);
        budgetResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        budgetResult.setRecommendations(List.of(
                "建议优先保障核心功能区预算投入",
                "减少非必要装饰性材料开支"
        ));
        budgetResult.setRisks(List.of(
                "同时追求高颜值和高收纳可能导致预算超支"
        ));
        budgetResult.setSummary("预算建议偏向控制风险并优先保证实用性");

        AgentResult safetyResult = new AgentResult();
        safetyResult.setAgentType(AgentType.SAFETY);
        safetyResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        safetyResult.setRecommendations(List.of(
                "建议采用圆角家具，降低宠物活动时的碰撞风险",
                "建议选择防滑和耐磨材料，提升长期使用安全性"
        ));
        safetyResult.setRisks(List.of(
                "尖角家具和不耐磨材料可能带来安全和维护风险"
        ));
        safetyResult.setSummary("安全建议偏向家庭适配和日常使用安全");

        AgentResult storageResult = new AgentResult();
        storageResult.setAgentType(AgentType.STORAGE);
        storageResult.setAgentExecutionStatus(AgentExecutionStatus.SUCCESS);
        storageResult.setRecommendations(List.of(
                "建议增加玄关和客厅储物柜，提升收纳能力",
                "优先使用封闭式柜体，减少积灰问题"
        ));
        storageResult.setRisks(List.of(
                "柜体过多可能压缩空间感"
        ));
        storageResult.setSummary("收纳建议偏向储物能力与空间平衡");

        List<AgentResult> results = List.of(
                layoutResult,
                budgetResult,
                safetyResult,
                storageResult
        );

        DecorationPlan plan = coordinatorAgent.execute(requirement, results);

        System.out.println("==== CoordinatorAgent Result ====");
        System.out.println("summary = " + plan.getSummary());
        System.out.println("keyRecommendations = " + plan.getKeyRecommendations());
        System.out.println("conflicts = " + plan.getConflicts());
        System.out.println("finalSuggestion = " + plan.getFinalSuggestion());
        System.out.println("=================================");

        assertNotNull(plan);
        assertNotNull(plan.getSummary());
        assertFalse(plan.getSummary().isBlank());

        assertNotNull(plan.getKeyRecommendations());
        assertFalse(plan.getKeyRecommendations().isEmpty());

        assertNotNull(plan.getConflicts());

        assertNotNull(plan.getFinalSuggestion());
        assertFalse(plan.getFinalSuggestion().isBlank());
    }
}
