package com.yzan.yzan_multi_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.persistence.mapper.RequirementStateRecordMapper;
import com.yzan.yzan_multi_agent.persistence.record.RequirementStateRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequirementStateService {

    private final RequirementStateRecordMapper requirementStateRecordMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, StructuredRequirement> stateCache = new ConcurrentHashMap<>();
    private volatile boolean initialized;

    public RequirementStateService(RequirementStateRecordMapper requirementStateRecordMapper,
                                   ObjectMapper objectMapper) {
        this.requirementStateRecordMapper = requirementStateRecordMapper;
        this.objectMapper = objectMapper;
    }

    public StructuredRequirement load(String sessionId) {
        ensureTableReady();
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        String normalizedSessionId = sessionId.trim();
        StructuredRequirement cachedState = stateCache.get(normalizedSessionId);
        if (cachedState != null) {
            return copy(cachedState);
        }

        RequirementStateRecord record = requirementStateRecordMapper.selectBySessionId(normalizedSessionId);
        if (record == null || record.getStructuredRequirementJson() == null || record.getStructuredRequirementJson().isBlank()) {
            return null;
        }

        try {
            StructuredRequirement state = objectMapper.readValue(record.getStructuredRequirementJson(), StructuredRequirement.class);
            stateCache.put(normalizedSessionId, copy(state));
            return state;
        } catch (Exception e) {
            throw new IllegalStateException("读取 StructuredRequirement 状态失败", e);
        }
    }

    public void save(String sessionId, StructuredRequirement structuredRequirement) {
        ensureTableReady();
        if (sessionId == null || sessionId.isBlank() || structuredRequirement == null) {
            return;
        }

        try {
            String normalizedSessionId = sessionId.trim();
            RequirementStateRecord existing = requirementStateRecordMapper.selectBySessionId(normalizedSessionId);
            RequirementStateRecord record = new RequirementStateRecord();
            record.setSessionId(normalizedSessionId);
            record.setStructuredRequirementJson(objectMapper.writeValueAsString(structuredRequirement));
            record.setVersion(existing == null || existing.getVersion() == null ? 1L : existing.getVersion());
            record.setCreatedAt(existing == null || existing.getCreatedAt() == null ? LocalDateTime.now() : existing.getCreatedAt());
            record.setUpdatedAt(LocalDateTime.now());
            requirementStateRecordMapper.upsert(record);
            stateCache.put(normalizedSessionId, copy(structuredRequirement));
        } catch (Exception e) {
            throw new IllegalStateException("保存 StructuredRequirement 状态失败", e);
        }
    }

    public void evict(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        stateCache.remove(sessionId.trim());
    }

    public void clearCache() {
        stateCache.clear();
    }

    private StructuredRequirement copy(StructuredRequirement state) {
        if (state == null) {
            return null;
        }
        return objectMapper.convertValue(state, StructuredRequirement.class);
    }

    private void ensureTableReady() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            requirementStateRecordMapper.createTableIfNotExists();
            requirementStateRecordMapper.createSessionIdIndexIfNotExists();
            initialized = true;
        }
    }
}
