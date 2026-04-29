package com.yzan.yzan_multi_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.IntentClassificationRequest;
import com.yzan.yzan_multi_agent.domain.IntentClassificationResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LlmUserIntentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParsePlanModificationFromLlmResult() {
        LlmUserIntentService service = new LlmUserIntentService(
                objectMapper,
                (memoryId, prompt) -> """
                        {
                          "intent": "PLAN_MODIFICATION",
                          "confidence": 0.92,
                          "reason": "用户在历史方案基础上修改预算。",
                          "requirementModification": true,
                          "normalizedUserRequest": "将上一版装修方案预算改为20万",
                          "skillType": "none",
                          "reply": ""
                        }
                        """
        );

        IntentClassificationRequest request = new IntentClassificationRequest();
        request.setSessionId("test-session");
        request.setUserRequest("这块再保守一点");

        IntentClassificationResult result = service.classify(request);

        assertThat(result.getIntent()).isEqualTo(UserIntentService.PLAN_MODIFICATION);
        assertThat(result.isRequirementModification()).isTrue();
        assertThat(result.getNormalizedUserRequest()).isEqualTo("将上一版装修方案预算改为20万");
    }

    @Test
    void shouldFallbackToGeneralChatWhenLlmFails() {
        LlmUserIntentService service = new LlmUserIntentService(
                objectMapper,
                (memoryId, prompt) -> {
                    throw new IllegalStateException("LLM unavailable");
                }
        );

        IntentClassificationRequest request = new IntentClassificationRequest();
        request.setSessionId("test-session");
        request.setUserRequest("你好");

        IntentClassificationResult result = service.classify(request);

        assertThat(result.getIntent()).isEqualTo(UserIntentService.GENERAL_CHAT);
        assertThat(result.getReply()).isNotBlank();
    }

    @Test
    void shouldUseFastPathForClearSmallTalkWithoutCallingLlm() {
        AtomicBoolean llmCalled = new AtomicBoolean(false);
        LlmUserIntentService service = new LlmUserIntentService(
                objectMapper,
                (memoryId, prompt) -> {
                    llmCalled.set(true);
                    throw new IllegalStateException("LLM should not be called");
                }
        );

        IntentClassificationRequest request = new IntentClassificationRequest();
        request.setSessionId("test-session");
        request.setUserRequest("你好");

        IntentClassificationResult result = service.classify(request);

        assertThat(result.getIntent()).isEqualTo(UserIntentService.GENERAL_CHAT);
        assertThat(result.getConfidence()).isGreaterThan(0.9);
        assertThat(result.getReply()).isNotBlank();
        assertThat(llmCalled).isFalse();
    }

    @Test
    void shouldUseFastPathForClearModificationWithoutCallingLlm() {
        AtomicBoolean llmCalled = new AtomicBoolean(false);
        LlmUserIntentService service = new LlmUserIntentService(
                objectMapper,
                (memoryId, prompt) -> {
                    llmCalled.set(true);
                    throw new IllegalStateException("LLM should not be called");
                }
        );

        IntentClassificationRequest request = new IntentClassificationRequest();
        request.setSessionId("test-session");
        request.setUserRequest("预算改成20万");

        IntentClassificationResult result = service.classify(request);

        assertThat(result.getIntent()).isEqualTo(UserIntentService.PLAN_MODIFICATION);
        assertThat(result.isRequirementModification()).isTrue();
        assertThat(llmCalled).isFalse();
    }
}
