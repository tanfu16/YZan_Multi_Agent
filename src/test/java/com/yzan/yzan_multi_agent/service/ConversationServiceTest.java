package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.ConversationResponse;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.skills.SkillExecutionResult;
import com.yzan.yzan_multi_agent.skills.SkillInvocationService;
import com.yzan.yzan_multi_agent.skills.SkillIntentRouter;
import com.yzan.yzan_multi_agent.workflow.PlanGenerationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationServiceTest {

    @Test
    void shouldReturnChatResponseWhenUnderstandingIsGeneralChat() {
        FakeRequirementUnderstandingService requirementService = new FakeRequirementUnderstandingService();
        FakePlanGenerationService planGenerationService = new FakePlanGenerationService();
        FakeSkillInvocationService skillInvocationService = new FakeSkillInvocationService();
        FakeConversationMemoryService conversationMemoryService = new FakeConversationMemoryService();
        ConversationService conversationService = new ConversationService(
                requirementService,
                planGenerationService,
                skillInvocationService,
                conversationMemoryService
        );

        UserRequirement request = new UserRequirement();
        request.setRawDescription("你好");

        RequirementUnderstandingResult understanding = new RequirementUnderstandingResult();
        understanding.setRawInput("你好");
        understanding.setIntentType(UserIntentType.GENERAL_CHAT);
        understanding.setReply("你好，我在。");
        requirementService.result = understanding;

        ConversationResponse response = conversationService.handle(request);

        assertThat(response.getIntentType()).isEqualTo(UserIntentType.GENERAL_CHAT);
        assertThat(response.getReply()).isEqualTo("你好，我在。");
        assertThat(planGenerationService.callCount).isZero();
        assertThat(skillInvocationService.callCount).isZero();
        assertThat(conversationMemoryService.recordCount).isEqualTo(1);
    }

    @Test
    void shouldExecuteWorkflowWhenUnderstandingIsPlanGeneration() {
        FakeRequirementUnderstandingService requirementService = new FakeRequirementUnderstandingService();
        FakePlanGenerationService planGenerationService = new FakePlanGenerationService();
        FakeSkillInvocationService skillInvocationService = new FakeSkillInvocationService();
        FakeConversationMemoryService conversationMemoryService = new FakeConversationMemoryService();
        ConversationService conversationService = new ConversationService(
                requirementService,
                planGenerationService,
                skillInvocationService,
                conversationMemoryService
        );

        UserRequirement request = new UserRequirement();
        request.setRawDescription("三室两厅，预算18万，想做现代简约");
        StructuredRequirement structuredRequirement = new StructuredRequirement();
        structuredRequirement.setHouseType("三室两厅");

        RequirementUnderstandingResult understanding = new RequirementUnderstandingResult();
        understanding.setRawInput(request.getRawDescription());
        understanding.setIntentType(UserIntentType.PLAN_GENERATION);
        understanding.setStructuredRequirement(structuredRequirement);
        requirementService.result = understanding;

        DecorationPlan plan = new DecorationPlan();
        plan.setSummary("测试方案");
        planGenerationService.plan = plan;

        ConversationResponse response = conversationService.handle(request);

        assertThat(response.getIntentType()).isEqualTo(UserIntentType.PLAN_GENERATION);
        assertThat(response.getDecorationPlan()).isSameAs(plan);
        assertThat(planGenerationService.callCount).isEqualTo(1);
        assertThat(planGenerationService.lastStructuredRequirement).isSameAs(structuredRequirement);
        assertThat(skillInvocationService.callCount).isZero();
        assertThat(conversationMemoryService.recordCount).isEqualTo(1);
    }

    @Test
    void shouldExecuteSkillWhenUnderstandingIsSkillCall() {
        FakeRequirementUnderstandingService requirementService = new FakeRequirementUnderstandingService();
        FakePlanGenerationService planGenerationService = new FakePlanGenerationService();
        FakeSkillInvocationService skillInvocationService = new FakeSkillInvocationService();
        FakeConversationMemoryService conversationMemoryService = new FakeConversationMemoryService();
        ConversationService conversationService = new ConversationService(
                requirementService,
                planGenerationService,
                skillInvocationService,
                conversationMemoryService
        );

        UserRequirement request = new UserRequirement();
        request.setRawDescription("帮我看看上海哪里能买瓷砖");

        RequirementUnderstandingResult understanding = new RequirementUnderstandingResult();
        understanding.setRawInput(request.getRawDescription());
        understanding.setIntentType(UserIntentType.SKILL_CALL);
        understanding.setSkillName(SkillIntentRouter.MATERIAL_STORE_SKILL);
        requirementService.result = understanding;

        SkillExecutionResult skillExecutionResult = new SkillExecutionResult();
        skillExecutionResult.setTriggered(true);
        skillExecutionResult.setMessage("Triggered material store search skill.");
        skillInvocationService.materialResult = skillExecutionResult;

        ConversationResponse response = conversationService.handle(request);

        assertThat(response.getIntentType()).isEqualTo(UserIntentType.SKILL_CALL);
        assertThat(response.getSkillExecutionResult()).isSameAs(skillExecutionResult);
        assertThat(skillInvocationService.callCount).isEqualTo(1);
        assertThat(skillInvocationService.lastLocation).isEqualTo("上海");
        assertThat(skillInvocationService.lastMaterialKeyword).isEqualTo("瓷砖");
        assertThat(planGenerationService.callCount).isZero();
        assertThat(conversationMemoryService.recordCount).isEqualTo(1);
    }

    private static class FakeRequirementUnderstandingService implements RequirementUnderstandingService {
        private RequirementUnderstandingResult result;

        @Override
        public RequirementUnderstandingResult understand(UserRequirement userRequirement) {
            return result;
        }
    }

    private static class FakePlanGenerationService implements PlanGenerationService {
        private DecorationPlan plan;
        private int callCount;
        private StructuredRequirement lastStructuredRequirement;

        @Override
        public DecorationPlan execute(UserRequirement userRequirement) {
            callCount++;
            return plan;
        }

        @Override
        public DecorationPlan execute(UserRequirement userRequirement, StructuredRequirement structuredRequirement) {
            callCount++;
            lastStructuredRequirement = structuredRequirement;
            return plan;
        }
    }

    private static class FakeSkillInvocationService implements SkillInvocationService {
        private SkillExecutionResult materialResult;
        private int callCount;
        private String lastLocation;
        private String lastMaterialKeyword;

        @Override
        public SkillExecutionResult execute(String userRequest, String location, String materialKeyword, String platform, String furnitureKeyword) {
            callCount++;
            return materialResult;
        }

        @Override
        public SkillExecutionResult executeMaterialStoreSkill(String userRequest, String location, String materialKeyword) {
            callCount++;
            lastLocation = location;
            lastMaterialKeyword = materialKeyword;
            return materialResult;
        }

        @Override
        public SkillExecutionResult executeFurnitureSearchSkill(String userRequest, String platform, String furnitureKeyword) {
            callCount++;
            return materialResult;
        }
    }

    private static class FakeConversationMemoryService extends ConversationMemoryService {
        private int recordCount;

        FakeConversationMemoryService() {
            super(null, null, null);
        }

        @Override
        public void recordTurnAndMaybeSummarize(UserRequirement userRequirement, ConversationResponse response) {
            recordCount++;
        }

        @Override
        public String loadLatestSummary(String sessionId) {
            return null;
        }
    }
}
