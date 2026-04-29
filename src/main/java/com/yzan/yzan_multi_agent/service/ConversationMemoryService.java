package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.ConversationResponse;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.persistence.mapper.ConversationSummaryRecordMapper;
import com.yzan.yzan_multi_agent.persistence.mapper.ConversationTurnRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.ConversationSummaryRecord;
import com.yzan.yzan_multi_agent.persistence.record.ConversationTurnRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationMemoryService {

    private static final int SHORT_TERM_WINDOW_SIZE = 20;
    private static final int SUMMARY_TRIGGER_THRESHOLD = 16;
    private static final int SUMMARY_LOOKBACK_TURNS = 16;
    private static final int SUMMARY_REBUILD_INTERVAL = 4;
    private static final int SUMMARY_INJECTION_LIMIT = 3;

    private final ConversationTurnRecordMapper conversationTurnRecordMapper;
    private final ConversationSummaryRecordMapper conversationSummaryRecordMapper;
    private final RequirementStateService requirementStateService;
    private volatile boolean initialized;

    public ConversationMemoryService(ConversationTurnRecordMapper conversationTurnRecordMapper,
                                     ConversationSummaryRecordMapper conversationSummaryRecordMapper,
                                     RequirementStateService requirementStateService) {
        this.conversationTurnRecordMapper = conversationTurnRecordMapper;
        this.conversationSummaryRecordMapper = conversationSummaryRecordMapper;
        this.requirementStateService = requirementStateService;
    }

    public void recordTurnAndMaybeSummarize(UserRequirement userRequirement, ConversationResponse response) {
        ensureTablesReady();
        String sessionId = normalizeSessionId(userRequirement == null ? null : userRequirement.getSessionId());
        if (sessionId == null) {
            return;
        }

        ConversationTurnRecord turnRecord = new ConversationTurnRecord();
        turnRecord.setSessionId(sessionId);
        turnRecord.setRawInput(userRequirement == null ? "" : safe(userRequirement.getRawDescription()));
        turnRecord.setAssistantOutput(buildAssistantOutput(response));
        turnRecord.setIntentType(response == null || response.getIntentType() == null ? null : response.getIntentType().name());
        turnRecord.setCreatedAt(LocalDateTime.now());
        conversationTurnRecordMapper.insert(turnRecord);

        int turnCount = conversationTurnRecordMapper.countBySessionId(sessionId);
        ConversationSummaryRecord existingSummary = conversationSummaryRecordMapper.selectLatestBySessionId(sessionId);
        int coveredTurns = existingSummary == null || existingSummary.getEndTurn() == null
                ? 0
                : existingSummary.getEndTurn();

        if (turnCount < SUMMARY_TRIGGER_THRESHOLD || (turnCount - coveredTurns) < SUMMARY_REBUILD_INTERVAL) {
            return;
        }

        int targetSegmentTurnCount = Math.min(turnCount - coveredTurns, SUMMARY_LOOKBACK_TURNS);
        List<ConversationTurnRecord> recentTurns = conversationTurnRecordMapper
                .selectBySessionIdAfterId(sessionId, coveredTurns, targetSegmentTurnCount);
        if (recentTurns.isEmpty()) {
            return;
        }
        StructuredRequirement currentState = requirementStateService.load(sessionId);
        int startTurn = coveredTurns + 1;
        int endTurn = coveredTurns + recentTurns.size();
        String summaryText = buildSummary(recentTurns, currentState, startTurn, endTurn, turnCount);

        ConversationSummaryRecord summaryRecord = new ConversationSummaryRecord();
        summaryRecord.setSessionId(sessionId);
        summaryRecord.setSummaryText(summaryText);
        summaryRecord.setStartTurn(startTurn);
        summaryRecord.setEndTurn(endTurn);
        summaryRecord.setCreatedAt(LocalDateTime.now());
        summaryRecord.setUpdatedAt(LocalDateTime.now());
        conversationSummaryRecordMapper.insertOrUpdate(summaryRecord);
    }

    public String loadLatestSummary(String sessionId) {
        ensureTablesReady();
        String normalizedSessionId = normalizeSessionId(sessionId);
        if (normalizedSessionId == null) {
            return null;
        }
        List<ConversationSummaryRecord> summaryRecords = conversationSummaryRecordMapper
                .selectRecentBySessionId(normalizedSessionId, SUMMARY_INJECTION_LIMIT);
        if (summaryRecords == null || summaryRecords.isEmpty()) {
            return null;
        }

        List<ConversationSummaryRecord> orderedRecords = new ArrayList<>(summaryRecords);
        Collections.reverse(orderedRecords);
        StringBuilder builder = new StringBuilder();
        for (ConversationSummaryRecord summaryRecord : orderedRecords) {
            builder.append("[长期记忆片段 ")
                    .append(summaryRecord.getStartTurn())
                    .append("-")
                    .append(summaryRecord.getEndTurn())
                    .append("] ")
                    .append(summaryRecord.getSummaryText())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    protected String buildSummary(List<ConversationTurnRecord> recentTurns,
                                  StructuredRequirement currentState,
                                  int startTurn,
                                  int endTurn,
                                  int turnCount) {
        return buildSummaryInternal(recentTurns, currentState, startTurn, endTurn, turnCount);
    }

    private String buildSummaryInternal(List<ConversationTurnRecord> recentTurns,
                                        StructuredRequirement currentState,
                                        int startTurn,
                                        int endTurn,
                                        int turnCount) {
        List<ConversationTurnRecord> turns = recentTurns == null ? Collections.emptyList() : recentTurns;
        StringBuilder summary = new StringBuilder();
        summary.append("这是第 ").append(startTurn).append(" 到 ").append(endTurn)
                .append(" 轮对话摘要。会话已累计 ").append(turnCount).append(" 轮。");

        if (currentState != null) {
            summary.append(" 当前已确认需求状态：");
            appendIfPresent(summary, "户型", currentState.getHouseType());
            appendIfPresent(summary, "面积", currentState.getArea());
            appendIfPresent(summary, "预算", currentState.getBudget());
            appendIfPresent(summary, "家庭画像", currentState.getFamilyProfile());
            appendIfPresent(summary, "风格", currentState.getStylePreference());
            appendIfPresent(summary, "优先级", currentState.getPriorities());
            appendIfPresent(summary, "约束", currentState.getConstraints());
        }

        if (!turns.isEmpty()) {
            summary.append(" 最近关键对话：");
            int limit = Math.min(turns.size(), 6);
            for (int i = Math.max(0, turns.size() - limit); i < turns.size(); i++) {
                ConversationTurnRecord turn = turns.get(i);
                summary.append("[")
                        .append(turn.getIntentType() == null ? "UNKNOWN" : turn.getIntentType())
                        .append("] 用户：")
                        .append(trim(turn.getRawInput(), 60));
                if (turn.getAssistantOutput() != null && !turn.getAssistantOutput().isBlank()) {
                    summary.append("；系统：").append(trim(turn.getAssistantOutput(), 80));
                }
                summary.append("。");
            }
        }

        return summary.toString();
    }

    private void appendIfPresent(StringBuilder summary, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (text.isBlank() || "[]".equals(text)) {
            return;
        }
        summary.append(label).append("=").append(text).append("；");
    }

    private String buildAssistantOutput(ConversationResponse response) {
        if (response == null) {
            return "";
        }
        if (response.getReply() != null && !response.getReply().isBlank()) {
            return response.getReply();
        }
        if (response.getDecorationPlan() != null && response.getDecorationPlan().getSummary() != null) {
            return response.getDecorationPlan().getSummary();
        }
        if (response.getSkillExecutionResult() != null && response.getSkillExecutionResult().getMessage() != null) {
            return response.getSkillExecutionResult().getMessage();
        }
        return "";
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? null : sessionId.trim();
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private void ensureTablesReady() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            conversationTurnRecordMapper.createTableIfNotExists();
            conversationTurnRecordMapper.createSessionIdIndexIfNotExists();
            conversationSummaryRecordMapper.createTableIfNotExists();
            conversationSummaryRecordMapper.addStartTurnColumnIfNotExists();
            conversationSummaryRecordMapper.addEndTurnColumnIfNotExists();
            conversationSummaryRecordMapper.backfillTurnRangeFromCoveredTurns();
            conversationSummaryRecordMapper.dropTurnCountCoveredColumnIfExists();
            conversationSummaryRecordMapper.dropLegacySessionUniqueIndexIfExists();
            conversationSummaryRecordMapper.createSessionIdIndexIfNotExists();
            conversationSummaryRecordMapper.createTurnRangeIndexIfNotExists();
            initialized = true;
        }
    }
}
