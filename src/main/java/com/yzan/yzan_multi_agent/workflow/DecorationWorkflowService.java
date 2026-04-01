package com.yzan.yzan_multi_agent.workflow;


import com.yzan.yzan_multi_agent.agent.*;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
public class DecorationWorkflowService {

    private final RequirementAgent requirementAgent = new RequirementAgent();
    private final DecorationAgent layoutAgent = new LayoutAgent();
    private final DecorationAgent budgetAgent = new BudgetAgent();
    private final DecorationAgent safetyAgent = new SafetyAgent();
    private final DecorationAgent storageAgent = new StorageAgent();
    private final CoordinatorAgent coordinatorAgent = new CoordinatorAgent();

    public DecorationPlan execute(UserRequirement userRequirement){
        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);

        // 四个并行 Agent 异步启动，不互相等待
        CompletableFuture<AgentResult> layoutFuture =
                CompletableFuture.supplyAsync(() -> layoutAgent.execute(structuredRequirement));
        CompletableFuture<AgentResult> budgetFuture =
                CompletableFuture.supplyAsync(() -> budgetAgent.execute(structuredRequirement));
        CompletableFuture<AgentResult> safetyFuture =
                CompletableFuture.supplyAsync(() -> safetyAgent.execute(structuredRequirement));
        CompletableFuture<AgentResult> storageFuture =
                CompletableFuture.supplyAsync(() -> storageAgent.execute(structuredRequirement));

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

}
