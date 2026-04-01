package com.yzan.yzan_multi_agent.workflow;


import com.yzan.yzan_multi_agent.agent.*;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
public class DecorationWorkflowService {

    private final RequirementAgent requirementAgent;
    private final LayoutAgent layoutAgent;
    private final BudgetAgent budgetAgent;
    private final SafetyAgent safetyAgent;
    private final StorageAgent storageAgent;
    private final CoordinatorAgent coordinatorAgent;

    public DecorationWorkflowService(
            RequirementAgent requirementAgent,
            LayoutAgent layoutAgent,
            BudgetAgent budgetAgent,
            SafetyAgent safetyAgent,
            StorageAgent storageAgent,
            CoordinatorAgent coordinatorAgent
    ) {
        this.requirementAgent = requirementAgent;
        this.layoutAgent = layoutAgent;
        this.budgetAgent = budgetAgent;
        this.safetyAgent = safetyAgent;
        this.storageAgent = storageAgent;
        this.coordinatorAgent = coordinatorAgent;
    }


    public DecorationPlan execute(UserRequirement userRequirement){
        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);

        // 四个并行 Agent 异步启动，不互相等待
        CompletableFuture<AgentResult> layoutFuture =
                CompletableFuture.supplyAsync(() -> layoutAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.LAYOUT, "布局分析超时，已降级"), 3, TimeUnit.SECONDS)
                        .exceptionally(ex -> buildFailedResult(AgentType.LAYOUT, "布局分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> budgetFuture =
                CompletableFuture.supplyAsync(() -> budgetAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.BUDGET, "预算分析超时，已降级"), 3, TimeUnit.SECONDS)
                        .exceptionally(ex -> buildFailedResult(AgentType.BUDGET, "预算分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> safetyFuture =
                CompletableFuture.supplyAsync(() -> safetyAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.SAFETY, "安全分析超时，已降级"), 3, TimeUnit.SECONDS)
                        .exceptionally(ex -> buildFailedResult(AgentType.SAFETY, "安全分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> storageFuture =
                CompletableFuture.supplyAsync(() -> storageAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.STORAGE, "收纳分析超时，已降级"), 3, TimeUnit.SECONDS)
                        .exceptionally(ex -> buildFailedResult(AgentType.STORAGE, "收纳分析失败，已返回降级结果"));


        // 等这 4 个任务全部结束，先执行完的 Agent 在此阻塞
        CompletableFuture.allOf(layoutFuture, budgetFuture, safetyFuture, storageFuture).join();

        // 取出结果
        List<AgentResult> results = List.of(
                layoutFuture.join(),
                budgetFuture.join(),
                safetyFuture.join(),
                storageFuture.join()
        );


        return coordinatorAgent.execute(structuredRequirement, results);
    }


    // 异常策略：本质是认为给 AgentResult 填充内容，包括名称、状态、消息等
    // 1、Agent 失败降级策略
    private AgentResult buildFailedResult(AgentType agentType, String message) {
        AgentResult result = new AgentResult();
        result.setAgentType(agentType);
        result.setAgentExecutionStatus(AgentExecutionStatus.FAILED);
        result.setRecommendations(List.of());
        result.setRisks(List.of(message));
        result.setSummary(message);
        return result;
    }

    // 2、Agent 超时策略
    private AgentResult buildTimeoutResult(AgentType agentType, String message) {
        AgentResult result = new AgentResult();
        result.setAgentType(agentType);
        result.setAgentExecutionStatus(AgentExecutionStatus.DEGRADED);
        result.setRecommendations(List.of());
        result.setRisks(List.of(message));
        result.setSummary(message);
        return result;
    }



}
