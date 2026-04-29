package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.service.ConversationMemoryService;
import com.yzan.yzan_multi_agent.service.PlanIntentService;
import com.yzan.yzan_multi_agent.service.RequirementStateService;
import com.yzan.yzan_multi_agent.service.StructuredRequirementStateMerger;
import com.yzan.yzan_multi_agent.skills.SkillIntentRouter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementAgentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SkillIntentRouter skillIntentRouter = new SkillIntentRouter();
    private final PlanIntentService planIntentService = new PlanIntentService();
    private final StructuredRequirementStateMerger stateMerger = new StructuredRequirementStateMerger();
    private final FakeConversationMemoryService conversationMemoryService = new FakeConversationMemoryService();

    @Test
    void shouldReturnStructuredPlanUnderstandingForPlanIntent() {
        InMemoryRequirementStateService requirementStateService = new InMemoryRequirementStateService(objectMapper);
        RequirementAgent requirementAgent = new RequirementAgent(
                objectMapper,
                (memoryId, prompt) -> """
                        {
                          "intentType": "PLAN_GENERATION",
                          "reply": "",
                          "skillName": "",
                          "structuredRequirementPatch": {
                            "familyProfile": { "operation": "SET", "value": "夫妻+孩子+宠物", "confidence": 0.98 },
                            "stylePreference": { "operation": "SET", "value": "现代简约", "confidence": 0.98 },
                            "priorities": { "operation": "ADD", "values": ["安全", "收纳", "好打理"], "confidence": 0.98 },
                            "constraints": { "operation": "ADD", "values": ["避免尖角", "重视防滑"], "confidence": 0.98 }
                          }
                        }
                        """,
                skillIntentRouter,
                planIntentService,
                requirementStateService,
                stateMerger,
                conversationMemoryService
        );

        UserRequirement userRequirement = buildPlanRequirement();

        RequirementUnderstandingResult result = requirementAgent.understand(userRequirement);

        assertThat(result.getIntentType()).isEqualTo(UserIntentType.PLAN_GENERATION);
        assertThat(result.getStructuredRequirement()).isNotNull();
        assertThat(result.getStructuredRequirement().getHouseType()).isEqualTo("三室两厅");
        assertThat(result.getStructuredRequirement().getArea()).isEqualTo(118);
        assertThat(result.getStructuredRequirement().getBudget()).isEqualByComparingTo("180000");
        assertThat(result.getStructuredRequirement().getFamilyProfile()).isEqualTo("夫妻+孩子+宠物");
        assertThat(requirementStateService.load(userRequirement.getSessionId()).getStylePreference()).isEqualTo("现代简约");
    }

    @Test
    void shouldReturnChatReplyForGeneralChatIntent() {
        InMemoryRequirementStateService requirementStateService = new InMemoryRequirementStateService(objectMapper);
        RequirementAgent requirementAgent = new RequirementAgent(
                objectMapper,
                (memoryId, prompt) -> """
                        {
                          "intentType": "GENERAL_CHAT",
                          "reply": "你好，我在。",
                          "skillName": "",
                          "structuredRequirementPatch": null
                        }
                        """,
                skillIntentRouter,
                planIntentService,
                requirementStateService,
                stateMerger,
                conversationMemoryService
        );

        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setRawDescription("你好");

        RequirementUnderstandingResult result = requirementAgent.understand(userRequirement);

        assertThat(result.getIntentType()).isEqualTo(UserIntentType.GENERAL_CHAT);
        assertThat(result.getReply()).isEqualTo("你好，我在。");
        assertThat(result.getStructuredRequirement()).isNull();
    }

    @Test
    void shouldFallbackToSkillCallWhenLlmFails() {
        InMemoryRequirementStateService requirementStateService = new InMemoryRequirementStateService(objectMapper);
        RequirementAgent requirementAgent = new RequirementAgent(
                objectMapper,
                (memoryId, prompt) -> {
                    throw new IllegalStateException("LLM unavailable");
                },
                skillIntentRouter,
                planIntentService,
                requirementStateService,
                stateMerger,
                conversationMemoryService
        );

        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setRawDescription("帮我看看上海哪里能买瓷砖");

        RequirementUnderstandingResult result = requirementAgent.understand(userRequirement);

        assertThat(result.getIntentType()).isEqualTo(UserIntentType.SKILL_CALL);
        assertThat(result.getSkillName()).isEqualTo(SkillIntentRouter.MATERIAL_STORE_SKILL);
        assertThat(result.getStructuredRequirement()).isNull();
    }

    @Test
    void shouldMergePatchWithExistingStructuredRequirementState() {
        InMemoryRequirementStateService requirementStateService = new InMemoryRequirementStateService(objectMapper);
        StructuredRequirement existing = new StructuredRequirement();
        existing.setHouseType("三室两厅");
        existing.setArea(118);
        existing.setBudget(new BigDecimal("180000"));
        existing.setStylePreference("现代简约");
        existing.setPriorities(List.of("安全", "收纳"));
        existing.setConstraints(List.of("防滑", "耐磨"));
        requirementStateService.save("session-1", existing);

        RequirementAgent requirementAgent = new RequirementAgent(
                objectMapper,
                (memoryId, prompt) -> """
                        {
                          "intentType": "PLAN_GENERATION",
                          "reply": "",
                          "skillName": "",
                          "structuredRequirementPatch": {
                            "budget": { "operation": "SET", "value": "200000", "confidence": 0.96 },
                            "stylePreference": { "operation": "KEEP", "confidence": 0.90 },
                            "constraints": { "operation": "REMOVE", "values": ["防滑"], "confidence": 0.95 },
                            "priorities": { "operation": "ADD", "values": ["好打理"], "confidence": 0.95 }
                          }
                        }
                        """,
                skillIntentRouter,
                planIntentService,
                requirementStateService,
                stateMerger,
                conversationMemoryService
        );

        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setSessionId("session-1");
        userRequirement.setRawDescription("预算改成20万，继续保持现代简约，再加一点好打理，不要防滑要求。");

        RequirementUnderstandingResult result = requirementAgent.understand(userRequirement);

        assertThat(result.getStructuredRequirement().getBudget()).isEqualByComparingTo("200000");
        assertThat(result.getStructuredRequirement().getStylePreference()).isEqualTo("现代简约");
        assertThat(result.getStructuredRequirement().getConstraints()).containsExactly("耐磨");
        assertThat(result.getStructuredRequirement().getPriorities()).containsExactly("安全", "收纳", "好打理");
    }

    private UserRequirement buildPlanRequirement() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("三室两厅");
        userRequirement.setArea(118);
        userRequirement.setBudget(new BigDecimal("180000"));
        userRequirement.setFamilyMembers(List.of("夫妻", "孩子", "宠物"));
        userRequirement.setStylePreference("现代简约");
        userRequirement.setSpecialNeeds(List.of("安全", "收纳", "好打理"));
        userRequirement.setRawDescription("预算18万，三室两厅，家里有夫妻、孩子和宠物，希望整体现代简约风。");
        return userRequirement;
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
            if (sessionId == null) {
                return;
            }
            states.put(sessionId, structuredRequirement);
        }
    }

    private static class FakeConversationMemoryService extends ConversationMemoryService {

        FakeConversationMemoryService() {
            super(null, null, null);
        }

        @Override
        public String loadLatestSummary(String sessionId) {
            return "历史摘要：用户之前已经确认预算和风格。";
        }

        @Override
        public void recordTurnAndMaybeSummarize(UserRequirement userRequirement, com.yzan.yzan_multi_agent.domain.ConversationResponse response) {
        }
    }
}
