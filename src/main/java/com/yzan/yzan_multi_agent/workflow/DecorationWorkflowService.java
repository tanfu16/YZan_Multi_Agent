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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
    private final ObjectMapper objectMapper;
    private final RequirementRecordMapper requirementRecordMapper;
    private final AgentExecutionRecordMapper agentExecutionRecordMapper;
    private final PlanRecordMapper planRecordMapper;


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
            PlanRecordMapper planRecordMapper
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
    }



    public DecorationPlan execute(UserRequirement userRequirement){
        String requestId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        String parentRequestId = null;
        String userId = null;

        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);

        // 构造 requirementRecord
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

        // 四个并行 Agent 异步启动，不互相等待
        CompletableFuture<AgentResult> layoutFuture =
                CompletableFuture.supplyAsync(() -> layoutAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.LAYOUT, "布局分析超时，已降级"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.LAYOUT, "布局分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> budgetFuture =
                CompletableFuture.supplyAsync(() -> budgetAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.BUDGET, "预算分析超时，已降级"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.BUDGET, "预算分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> safetyFuture =
                CompletableFuture.supplyAsync(() -> safetyAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.SAFETY, "安全分析超时，已降级"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.SAFETY, "安全分析失败，已返回降级结果"));
        CompletableFuture<AgentResult> storageFuture =
                CompletableFuture.supplyAsync(() -> storageAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.STORAGE, "收纳分析超时，已降级"), 3, TimeUnit.MINUTES)
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

        // 构造 layoutAgent
        AgentExecutionRecord layoutAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, layoutFuture.join());
        // 构造 budgetAgent
        AgentExecutionRecord budgetAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, budgetFuture.join());
        // 构造 safetyAgent
        AgentExecutionRecord safetyAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, safetyFuture.join());
        // 构造 storageAgent
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

        // 构造 planRecord
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




    // Requirement 记忆持久化
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


    // 并行 Agent 记忆持久化
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
            throw new RuntimeException("构建 RequirementRecord 失败", e);
        }
    }

    // CoordinatorAgent 记忆持久化
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
