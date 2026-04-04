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
        String sessionId = (userRequirement.getSessionId() != null && !userRequirement.getSessionId().isBlank()) ? userRequirement.getSessionId().trim() : UUID.randomUUID().toString();
        String parentRequestId = null;
        String userId = null;

        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);

        // 鏋勯€?requirementRecord
        RequirementRecord requirementRecord = buildRequirementRecord(
                requestId,
                sessionId,
                parentRequestId,
                userId,
                userRequirement,
                structuredRequirement
        );
        // requirementRecord 鍏ュ簱
        requirementRecordMapper.insert(requirementRecord);
        System.out.println("RequirementRecord saved, id = " + requirementRecord.getId());

        // 鍥涗釜骞惰 Agent 寮傛鍚姩锛屼笉浜掔浉绛夊緟
        CompletableFuture<AgentResult> layoutFuture =
                CompletableFuture.supplyAsync(() -> layoutAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.LAYOUT, "甯冨眬鍒嗘瀽瓒呮椂锛屽凡闄嶇骇"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.LAYOUT, "甯冨眬鍒嗘瀽澶辫触锛屽凡杩斿洖闄嶇骇缁撴灉"));
        CompletableFuture<AgentResult> budgetFuture =
                CompletableFuture.supplyAsync(() -> budgetAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.BUDGET, "棰勭畻鍒嗘瀽瓒呮椂锛屽凡闄嶇骇"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.BUDGET, "棰勭畻鍒嗘瀽澶辫触锛屽凡杩斿洖闄嶇骇缁撴灉"));
        CompletableFuture<AgentResult> safetyFuture =
                CompletableFuture.supplyAsync(() -> safetyAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.SAFETY, "瀹夊叏鍒嗘瀽瓒呮椂锛屽凡闄嶇骇"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.SAFETY, "瀹夊叏鍒嗘瀽澶辫触锛屽凡杩斿洖闄嶇骇缁撴灉"));
        CompletableFuture<AgentResult> storageFuture =
                CompletableFuture.supplyAsync(() -> storageAgent.execute(structuredRequirement))
                        .completeOnTimeout(buildTimeoutResult(AgentType.STORAGE, "鏀剁撼鍒嗘瀽瓒呮椂锛屽凡闄嶇骇"), 3, TimeUnit.MINUTES)
                        .exceptionally(ex -> buildFailedResult(AgentType.STORAGE, "鏀剁撼鍒嗘瀽澶辫触锛屽凡杩斿洖闄嶇骇缁撴灉"));

        // 绛夎繖 4 涓换鍔″叏閮ㄧ粨鏉燂紝鍏堟墽琛屽畬鐨?Agent 鍦ㄦ闃诲
        CompletableFuture.allOf(layoutFuture, budgetFuture, safetyFuture, storageFuture).join();

        // 鍙栧嚭缁撴灉
        List<AgentResult> results = List.of(
                layoutFuture.join(),
                budgetFuture.join(),
                safetyFuture.join(),
                storageFuture.join()
        );

        // 鏋勯€?layoutAgent
        AgentExecutionRecord layoutAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, layoutFuture.join());
        // 鏋勯€?budgetAgent
        AgentExecutionRecord budgetAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, budgetFuture.join());
        // 鏋勯€?safetyAgent
        AgentExecutionRecord safetyAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, safetyFuture.join());
        // 鏋勯€?storageAgent
        AgentExecutionRecord storageAgentExecutionRecord = buildAgentExecutionRecord(requestId, sessionId,
                parentRequestId, userId, structuredRequirement, storageFuture.join());

        // AgentRecord 鍏ュ簱
        agentExecutionRecordMapper.insert(layoutAgentExecutionRecord);
        System.out.println("layoutAgentExecutionRecord saved, id = " + layoutAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(budgetAgentExecutionRecord);
        System.out.println("budgetAgentExecutionRecord saved, id = " + budgetAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(safetyAgentExecutionRecord);
        System.out.println("safetyAgentExecutionRecord saved, id = " + safetyAgentExecutionRecord.getId());
        agentExecutionRecordMapper.insert(storageAgentExecutionRecord);
        System.out.println("storageAgentExecutionRecord saved, id = " + storageAgentExecutionRecord.getId());


        DecorationPlan decorationPlan = coordinatorAgent.execute(structuredRequirement, results);

        // 鏋勯€?planRecord
        PlanRecord planRecord = buildPlanRecord(
                requestId,
                sessionId,
                parentRequestId,
                userId,
                structuredRequirement,
                results,
                decorationPlan
        );
        // planRecord 鍏ュ簱
        planRecordMapper.insert(planRecord);
        System.out.println("PlanRecord saved, id = " + planRecord.getId());


        return decorationPlan;
    }




    // Requirement 璁板繂鎸佷箙鍖?
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
            throw new RuntimeException("鏋勫缓 RequirementRecord 澶辫触", e);
        }
    }


    // 骞惰 Agent 璁板繂鎸佷箙鍖?
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
            throw new RuntimeException("鏋勫缓 RequirementRecord 澶辫触", e);
        }
    }

    // CoordinatorAgent 璁板繂鎸佷箙鍖?
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
            throw new RuntimeException("鏋勫缓 PlanRecord 澶辫触", e);
        }
    }

    // 寮傚父绛栫暐锛氭湰璐ㄦ槸璁や负缁?AgentResult 濉厖鍐呭锛屽寘鎷悕绉般€佺姸鎬併€佹秷鎭瓑
    // 1銆丄gent 澶辫触闄嶇骇绛栫暐
    private AgentResult buildFailedResult(AgentType agentType, String message) {
        AgentResult result = new AgentResult();
        result.setAgentType(agentType);
        result.setAgentExecutionStatus(AgentExecutionStatus.FAILED);
        result.setRecommendations(List.of());
        result.setRisks(List.of(message));
        result.setSummary(message);
        return result;
    }

    // 2銆丄gent 瓒呮椂绛栫暐
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

