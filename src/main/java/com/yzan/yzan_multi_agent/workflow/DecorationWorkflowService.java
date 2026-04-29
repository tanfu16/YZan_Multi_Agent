package com.yzan.yzan_multi_agent.workflow;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.agent.*;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentExecutionStatus;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import com.yzan.yzan_multi_agent.persistence.mapper.AgentExecutionRecordMapper;
import com.yzan.yzan_multi_agent.persistence.mapper.PlanRecordMapper;
import com.yzan.yzan_multi_agent.persistence.mapper.RequirementRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.AgentExecutionRecord;
import com.yzan.yzan_multi_agent.persistence.record.PlanRecord;
import com.yzan.yzan_multi_agent.persistence.record.RequirementRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Service
public class DecorationWorkflowService implements PlanGenerationService {

    private final RequirementAgent requirementAgent;
    private final LayoutAgent layoutAgent;
    private final BudgetAgent budgetAgent;
    private final SafetyAgent safetyAgent;
    private final StorageAgent storageAgent;
    private final CoordinatorAgent coordinatorAgent;
    private final ObjectMapper objectMapper;
    private final RequirementRecordMapper requirementRecordMapper;
    private final AgentExecutionRecordMapper agentExecutionRecordMapper;
    private final PlanRecordMapper planRecordMapper;
    private final ThreadPoolExecutor agentWorkflowThreadPoolExecutor;


    public DecorationWorkflowService(
            RequirementAgent requirementAgent,
            LayoutAgent layoutAgent,
            BudgetAgent budgetAgent,
            SafetyAgent safetyAgent,
            StorageAgent storageAgent,
            CoordinatorAgent coordinatorAgent,
            ObjectMapper objectMapper,
            RequirementRecordMapper requirementRecordMapper,
            AgentExecutionRecordMapper agentExecutionRecordMapper,
            PlanRecordMapper planRecordMapper,
            @Qualifier("agentWorkflowThreadPoolExecutor") ThreadPoolExecutor agentWorkflowThreadPoolExecutor
    ) {
        this.requirementAgent = requirementAgent;
        this.layoutAgent = layoutAgent;
        this.budgetAgent = budgetAgent;
        this.safetyAgent = safetyAgent;
        this.storageAgent = storageAgent;
        this.coordinatorAgent = coordinatorAgent;
        this.objectMapper = objectMapper;
        this.requirementRecordMapper = requirementRecordMapper;
        this.agentExecutionRecordMapper = agentExecutionRecordMapper;
        this.planRecordMapper = planRecordMapper;
        this.agentWorkflowThreadPoolExecutor = agentWorkflowThreadPoolExecutor;
    }



    @Override
    public DecorationPlan execute(UserRequirement userRequirement){
        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);
        return execute(userRequirement, structuredRequirement);
    }

    @Override
    public DecorationPlan execute(UserRequirement userRequirement, StructuredRequirement structuredRequirement){
        String requestId = UUID.randomUUID().toString();
        String sessionId = (userRequirement.getSessionId() != null && !userRequirement.getSessionId().isBlank()) ? userRequirement.getSessionId().trim() : UUID.randomUUID().toString();
        String parentRequestId = null;
        String userId = null;

        // 构建 requirementRecord
        RequirementRecord requirementRecord = buildRequirementRecord(
                requestId,
                sessionId,
                parentRequestId,
                userId,
                userRequirement,
                structuredRequirement
        );
        // requirementRecord 入库
        requirementRecordMapper.insert(requirementRecord);
        System.out.println("RequirementRecord saved, id = " + requirementRecord.getId());

        // 四个并行 Agent 显式提交到 ThreadPoolExecutor 执行。
        CompletableFuture<AgentResult> layoutFuture =
                submitAgentTask(() -> layoutAgent.execute(structuredRequirement), AgentType.LAYOUT, "布局分析");
        CompletableFuture<AgentResult> budgetFuture =
                submitAgentTask(() -> budgetAgent.execute(structuredRequirement), AgentType.BUDGET, "预算分析");
        CompletableFuture<AgentResult> safetyFuture =
                submitAgentTask(() -> safetyAgent.execute(structuredRequirement), AgentType.SAFETY, "安全分析");
        CompletableFuture<AgentResult> storageFuture =
                submitAgentTask(() -> storageAgent.execute(structuredRequirement), AgentType.STORAGE, "收纳分析");

        // 等待 4 个任务全部结束，先执行完的 Agent 会在这里汇合
        CompletableFuture.allOf(layoutFuture, budgetFuture, safetyFuture, storageFuture).join();

        // 取出结果
        List<AgentResult> results = List.of(
                layoutFuture.join(),
                budgetFuture.join(),
                safetyFuture.join(),
                storageFuture.join()
        );

        // 构建 layoutAgent 执行记录
        AgentExecutionRecord layoutAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, layoutFuture.join());
        // 构建 budgetAgent 执行记录
        AgentExecutionRecord budgetAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, budgetFuture.join());
        // 构建 safetyAgent 执行记录
        AgentExecutionRecord safetyAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, safetyFuture.join());
        // 构建 storageAgent 执行记录
        AgentExecutionRecord storageAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, storageFuture.join());

        // AgentRecord 入库
        agentExecutionRecordMapper.insert(layoutAgentExecutionRecord);
        System.out.println("layoutAgentExecutionRecord saved, id = " + layoutAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(budgetAgentExecutionRecord);
        System.out.println("budgetAgentExecutionRecord saved, id = " + budgetAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(safetyAgentExecutionRecord);
        System.out.println("safetyAgentExecutionRecord saved, id = " + safetyAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(storageAgentExecutionRecord);
        System.out.println("storageAgentExecutionRecord saved, id = " + storageAgentExecutionRecord.getId());


        DecorationPlan decorationPlan = coordinatorAgent.execute(structuredRequirement, results);

        // 构建 planRecord
        PlanRecord planRecord = buildPlanRecord(
                requestId,
                sessionId,
                parentRequestId,
                userId,
                structuredRequirement,
                results,
                decorationPlan
        );
        // planRecord 入库
        planRecordMapper.insert(planRecord);
        System.out.println("PlanRecord saved, id = " + planRecord.getId());


        return decorationPlan;
    }




    // 持久化原始需求与结构化需求
    private RequirementRecord buildRequirementRecord(
            String requestId,
            String sessionId,
            String parentRequestId,
            String userId,
            UserRequirement userRequirement,
            StructuredRequirement structuredRequirement
    ) {
        try {
            RequirementRecord record = new RequirementRecord();
            record.setRequestId(requestId);
            record.setSessionId(sessionId);
            record.setParentRequestId(parentRequestId);
            record.setUserId(userId);
            record.setUserRequirementJson(objectMapper.writeValueAsString(userRequirement));
            record.setStructuredRequirementJson(objectMapper.writeValueAsString(structuredRequirement));
            record.setCreatedAt(LocalDateTime.now());
            return record;
        } catch (Exception e) {
            throw new RuntimeException("构建 RequirementRecord 失败", e);
        }
    }


    // 持久化并行 Agent 执行结果
    private AgentExecutionRecord buildAgentExecutionRecord(
            String requestId,
            String sessionId,
            String parentRequestId,
            String userId,
            StructuredRequirement structuredRequirement,
            AgentResult agentResult
    ) {
        try {
            AgentExecutionRecord record = new AgentExecutionRecord();
            record.setRequestId(requestId);
            record.setSessionId(sessionId);
            record.setParentRequestId(parentRequestId);
            record.setUserId(userId);
            record.setStructuredRequirementJson(objectMapper.writeValueAsString(structuredRequirement));
            record.setAgentResultJson(objectMapper.writeValueAsString(agentResult));
            record.setCreatedAt(LocalDateTime.now());
            return record;
        } catch (Exception e) {
            throw new RuntimeException("构建 AgentExecutionRecord 失败", e);
        }
    }

    // 持久化 CoordinatorAgent 生成的最终方案
    private PlanRecord buildPlanRecord(
            String requestId,
            String sessionId,
            String parentRequestId,
            String userId,
            StructuredRequirement structuredRequirement,
            List<AgentResult> agentResults,
            DecorationPlan decorationPlan
    ) {
        try {
            PlanRecord record = new PlanRecord();
            record.setRequestId(requestId);
            record.setSessionId(sessionId);
            record.setParentRequestId(parentRequestId);
            record.setUserId(userId);
            record.setStructuredRequirementJson(objectMapper.writeValueAsString(structuredRequirement));
            record.setAgentResultsJson(objectMapper.writeValueAsString(agentResults));
            record.setDecorationPlanJson(objectMapper.writeValueAsString(decorationPlan));
            record.setCreatedAt(LocalDateTime.now());
            return record;
        } catch (Exception e) {
            throw new RuntimeException("构建 PlanRecord 失败", e);
        }
    }

    // 异常策略：为失败或超时的 Agent 填充统一 AgentResult，避免阻断整体流程。
    private CompletableFuture<AgentResult> submitAgentTask(java.util.function.Supplier<AgentResult> task,
                                                           AgentType agentType,
                                                           String agentLabel) {
        return CompletableFuture.supplyAsync(task, agentWorkflowThreadPoolExecutor)
                .completeOnTimeout(buildTimeoutResult(agentType, agentLabel + "超时，已降级"), 3, TimeUnit.MINUTES)
                .exceptionally(ex -> buildFailedResult(agentType, agentLabel + "失败，已返回降级结果"));
    }

    // 1. Agent 失败降级策略
    private AgentResult buildFailedResult(AgentType agentType, String message) {
        AgentResult result = new AgentResult();
        result.setAgentType(agentType);
        result.setAgentExecutionStatus(AgentExecutionStatus.FAILED);
        result.setRecommendations(List.of());
        result.setRisks(List.of(message));
        result.setSummary(message);
        return result;
    }

    // 2. Agent 超时降级策略
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
