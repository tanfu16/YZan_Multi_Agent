package com.yzan.yzan_multi_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.persistence.mapper.RequirementStateRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.RequirementStateRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementStateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReadFromCacheAfterFirstDatabaseLoad() throws Exception {
        FakeRequirementStateRecordMapper mapper = new FakeRequirementStateRecordMapper();
        mapper.store("session-1", buildState("三室两厅", "180000"), objectMapper);
        RequirementStateService service = new RequirementStateService(mapper, objectMapper);

        StructuredRequirement firstLoad = service.load("session-1");
        StructuredRequirement secondLoad = service.load("session-1");

        assertThat(firstLoad.getHouseType()).isEqualTo("三室两厅");
        assertThat(secondLoad.getBudget()).isEqualByComparingTo("180000");
        assertThat(mapper.selectCount).isEqualTo(1);
    }

    @Test
    void shouldUpdateCacheWhenSavingState() {
        FakeRequirementStateRecordMapper mapper = new FakeRequirementStateRecordMapper();
        RequirementStateService service = new RequirementStateService(mapper, objectMapper);

        StructuredRequirement state = buildState("两室一厅", "120000");
        service.save("session-2", state);
        StructuredRequirement loaded = service.load("session-2");

        assertThat(loaded.getHouseType()).isEqualTo("两室一厅");
        assertThat(loaded.getBudget()).isEqualByComparingTo("120000");
        assertThat(mapper.upsertCount).isEqualTo(1);
        assertThat(mapper.selectCount).isEqualTo(1);
    }

    @Test
    void shouldFallbackToDatabaseAfterEvictingCache() throws Exception {
        FakeRequirementStateRecordMapper mapper = new FakeRequirementStateRecordMapper();
        mapper.store("session-3", buildState("四室两厅", "260000"), objectMapper);
        RequirementStateService service = new RequirementStateService(mapper, objectMapper);

        service.load("session-3");
        service.evict("session-3");
        StructuredRequirement reloaded = service.load("session-3");

        assertThat(reloaded.getHouseType()).isEqualTo("四室两厅");
        assertThat(mapper.selectCount).isEqualTo(2);
    }

    private StructuredRequirement buildState(String houseType, String budget) {
        StructuredRequirement state = new StructuredRequirement();
        state.setHouseType(houseType);
        state.setArea(118);
        state.setBudget(new BigDecimal(budget));
        state.setStylePreference("现代简约");
        return state;
    }

    private static class FakeRequirementStateRecordMapper implements RequirementStateRecordMapper {

        private final Map<String, RequirementStateRecord> records = new HashMap<>();
        private int selectCount;
        private int upsertCount;

        @Override
        public void createTableIfNotExists() {
        }

        @Override
        public void createSessionIdIndexIfNotExists() {
        }

        @Override
        public RequirementStateRecord selectBySessionId(String sessionId) {
            selectCount++;
            return records.get(sessionId);
        }

        @Override
        public int upsert(RequirementStateRecord record) {
            upsertCount++;
            RequirementStateRecord stored = new RequirementStateRecord();
            stored.setId(record.getId());
            stored.setSessionId(record.getSessionId());
            stored.setStructuredRequirementJson(record.getStructuredRequirementJson());
            stored.setVersion(record.getVersion() == null ? 1L : record.getVersion() + 1);
            stored.setCreatedAt(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt());
            stored.setUpdatedAt(record.getUpdatedAt() == null ? LocalDateTime.now() : record.getUpdatedAt());
            records.put(record.getSessionId(), stored);
            return 1;
        }

        void store(String sessionId, StructuredRequirement state, ObjectMapper objectMapper) throws Exception {
            RequirementStateRecord record = new RequirementStateRecord();
            record.setSessionId(sessionId);
            record.setStructuredRequirementJson(objectMapper.writeValueAsString(state));
            record.setVersion(1L);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            records.put(sessionId, record);
        }
    }
}
