package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.ConflictItem;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;

import java.util.ArrayList;
import java.util.List;

/**
 * 方案总控
 * 汇总多个专业 agent 的结果，识别冲突并生成最终方案
 */
public class CoordinatorAgent {

    public DecorationPlan execute(StructuredRequirement requirement, List<AgentResult> results){
        DecorationPlan plan = new DecorationPlan();

        List<String> recommendations = collectRecommendations(results);
        List<ConflictItem> conflicts = detectConflicts(results);

        plan.setSummary(buildSummary(requirement));
        plan.setKeyRecommendations(recommendations);
        plan.setConflicts(conflicts);
        plan.setFinalSuggestion(buildFinalSuggestion(conflicts));

        return plan;
    }

    // 收集各个Agent的建议
    private List<String> collectRecommendations(List<AgentResult> results) {
        List<String> recommendations = new ArrayList<>();
        for (AgentResult result : results) {
            if (result.getRecommendations() != null) {
                recommendations.addAll(result.getRecommendations());
            }
        }
        return recommendations;
    }

    // 定位冲突
    private List<ConflictItem> detectConflicts(List<AgentResult> results) {
        List<ConflictItem> conflicts = new ArrayList<>();

        boolean hasLayout = false;
        boolean hasSafety = false;

        for (AgentResult result : results) {
            if (result.getAgentType() == AgentType.LAYOUT) {
                hasLayout = true;
            }
            if (result.getAgentType() == AgentType.SAFETY) {
                hasSafety = true;
            }
        }

        if (hasLayout && hasSafety) {
            ConflictItem conflictItem = new ConflictItem();
            conflictItem.setTopic("空间通透性与家庭安全性冲突");
            conflictItem.setRelatedAgents(List.of("LAYOUT", "SAFETY"));
            conflictItem.setDescription("开放式布局更通透，但儿童家庭更需要控制尖角和开放区域风险。");
            conflictItem.setResolution("保留空间通透感，同时采用圆角家具和更安全的柜体设计。");
            conflicts.add(conflictItem);
        }

        return conflicts;
    }

    // 总结
    private String buildSummary(StructuredRequirement requirement) {
        return "该方案基于户型、预算、家庭结构和风格偏好进行了多 agent 协同分析。";
    }

    // 生成最终建议
    private String buildFinalSuggestion(List<ConflictItem> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "建议直接采用综合方案，优先兼顾实用性与舒适度。";
        }
        return "建议采用平衡型方案，在通透性、美观和家庭安全之间做折中。";
    }
}
