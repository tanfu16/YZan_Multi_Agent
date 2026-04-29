package com.yzan.yzan_multi_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.ConversationResponse;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.persistence.mapper.ConversationSummaryRecordMapper;
import com.yzan.yzan_multi_agent.persistence.mapper.ConversationTurnRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.ConversationSummaryRecord;
import com.yzan.yzan_multi_agent.persistence.record.ConversationTurnRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldGenerateAndLoadSummaryAfterThreshold() {
        FakeConversationTurnRecordMapper turnMapper = new FakeConversationTurnRecordMapper();
        FakeConversationSummaryRecordMapper summaryMapper = new FakeConversationSummaryRecordMapper();
        InMemoryRequirementStateService stateService = new InMemoryRequirementStateService(objectMapper);
        StructuredRequirement state = new StructuredRequirement();
        state.setHouseType("三室两厅");
        state.setBudget(new BigDecimal("180000"));
        state.setStylePreference("现代简约");
        stateService.save("session-1", state);

        ConversationMemoryService memoryService = new ConversationMemoryService(turnMapper, summaryMapper, stateService);

        for (int i = 1; i <= 16; i++) {
            UserRequirement request = new UserRequirement();
            request.setSessionId("session-1");
            request.setRawDescription("第" + i + "轮用户输入");

            ConversationResponse response = new ConversationResponse();
            response.setIntentType(UserIntentType.PLAN_GENERATION);
            response.setReply("第" + i + "轮回复");

            memoryService.recordTurnAndMaybeSummarize(request, response);
        }

        String summary = memoryService.loadLatestSummary("session-1");

        assertThat(summary).isNotBlank();
        assertThat(summary).contains("长期记忆片段 1-16");
        assertThat(summary).contains("会话已累计 16 轮");
        assertThat(summary).contains("户型=三室两厅");
        assertThat(summary).contains("预算=180000");
        assertThat(summaryMapper.records).hasSize(1);
        assertThat(summaryMapper.records.getFirst().getStartTurn()).isEqualTo(1);
        assertThat(summaryMapper.records.getFirst().getEndTurn()).isEqualTo(16);
    }

    @Test
    void shouldAppendSegmentedLongTermSummaries() {
        FakeConversationTurnRecordMapper turnMapper = new FakeConversationTurnRecordMapper();
        FakeConversationSummaryRecordMapper summaryMapper = new FakeConversationSummaryRecordMapper();
        InMemoryRequirementStateService stateService = new InMemoryRequirementStateService(objectMapper);

        ConversationMemoryService memoryService = new ConversationMemoryService(turnMapper, summaryMapper, stateService);

        for (int i = 1; i <= 20; i++) {
            UserRequirement request = new UserRequirement();
            request.setSessionId("session-2");
            request.setRawDescription("第" + i + "轮用户输入");

            ConversationResponse response = new ConversationResponse();
            response.setIntentType(UserIntentType.GENERAL_CHAT);
            response.setReply("第" + i + "轮回复");

            memoryService.recordTurnAndMaybeSummarize(request, response);
        }

        String summary = memoryService.loadLatestSummary("session-2");

        assertThat(summaryMapper.records).hasSize(2);
        assertThat(summaryMapper.records.get(0).getStartTurn()).isEqualTo(1);
        assertThat(summaryMapper.records.get(0).getEndTurn()).isEqualTo(16);
        assertThat(summaryMapper.records.get(1).getStartTurn()).isEqualTo(17);
        assertThat(summaryMapper.records.get(1).getEndTurn()).isEqualTo(20);
        assertThat(summary).contains("长期记忆片段 1-16");
        assertThat(summary).contains("长期记忆片段 17-20");
    }

    private static class FakeConversationTurnRecordMapper implements ConversationTurnRecordMapper {

        private final List<ConversationTurnRecord> records = new ArrayList<>();
        private long nextId = 1;

        @Override
        public void createTableIfNotExists() {
        }

        @Override
        public void createSessionIdIndexIfNotExists() {
        }

        @Override
        public int insert(ConversationTurnRecord record) {
            record.setId(nextId++);
            records.add(record);
            return 1;
        }

        @Override
        public int countBySessionId(String sessionId) {
            return (int) records.stream().filter(item -> sessionId.equals(item.getSessionId())).count();
        }

        @Override
        public List<ConversationTurnRecord> selectRecentBySessionId(String sessionId, int limit) {
            return records.stream()
                    .filter(item -> sessionId.equals(item.getSessionId()))
                    .sorted(Comparator.comparing(ConversationTurnRecord::getId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<ConversationTurnRecord> selectBySessionIdAfterId(String sessionId, long afterId, int limit) {
            return records.stream()
                    .filter(item -> sessionId.equals(item.getSessionId()))
                    .filter(item -> item.getId() != null && item.getId() > afterId)
                    .sorted(Comparator.comparing(ConversationTurnRecord::getId))
                    .limit(limit)
                    .toList();
        }
    }

    private static class FakeConversationSummaryRecordMapper implements ConversationSummaryRecordMapper {

        private final List<ConversationSummaryRecord> records = new ArrayList<>();

        @Override
        public void createTableIfNotExists() {
        }

        @Override
        public void createSessionIdIndexIfNotExists() {
        }

        @Override
        public void createTurnRangeIndexIfNotExists() {
        }

        @Override
        public void addStartTurnColumnIfNotExists() {
        }

        @Override
        public void addEndTurnColumnIfNotExists() {
        }

        @Override
        public void backfillTurnRangeFromCoveredTurns() {
        }

        @Override
        public void dropTurnCountCoveredColumnIfExists() {
        }

        @Override
        public void dropLegacySessionUniqueIndexIfExists() {
        }

        @Override
        public ConversationSummaryRecord selectLatestBySessionId(String sessionId) {
            return records.stream()
                    .filter(item -> sessionId.equals(item.getSessionId()))
                    .max(Comparator.comparing(ConversationSummaryRecord::getEndTurn))
                    .orElse(null);
        }

        @Override
        public List<ConversationSummaryRecord> selectRecentBySessionId(String sessionId, int limit) {
            return records.stream()
                    .filter(item -> sessionId.equals(item.getSessionId()))
                    .sorted(Comparator.comparing(ConversationSummaryRecord::getEndTurn).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public int insertOrUpdate(ConversationSummaryRecord record) {
            records.removeIf(item -> sessionIdAndRangeEquals(item, record));
            records.add(record);
            return 1;
        }

        private boolean sessionIdAndRangeEquals(ConversationSummaryRecord left, ConversationSummaryRecord right) {
            return left.getSessionId().equals(right.getSessionId())
                    && left.getStartTurn().equals(right.getStartTurn())
                    && left.getEndTurn().equals(right.getEndTurn());
        }
    }

    private static class InMemoryRequirementStateService extends RequirementStateService {

        private final Map<String, StructuredRequirement> states = new HashMap<>();

        InMemoryRequirementStateService(ObjectMapper objectMapper) {
            super(null, objectMapper);
        }

        @Override
        public StructuredRequirement load(String sessionId) {
            return states.get(sessionId);
        }

        @Override
        public void save(String sessionId, StructuredRequirement structuredRequirement) {
            states.put(sessionId, structuredRequirement);
        }
    }
}
