package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanIntentServiceTest {

    private final PlanIntentService planIntentService = new PlanIntentService();

    @Test
    void shouldRejectSmallTalk() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setRawDescription("你好");

        assertThat(planIntentService.isDecorationPlanIntent(userRequirement)).isFalse();
    }

    @Test
    void shouldAcceptDecorationText() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setRawDescription("三室两厅，预算18万，想做现代简约装修");

        assertThat(planIntentService.isDecorationPlanIntent(userRequirement)).isTrue();
    }

    @Test
    void shouldAcceptStructuredFields() {
        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setRawDescription("帮我看看");
        userRequirement.setBudget(new BigDecimal("180000"));

        assertThat(planIntentService.isDecorationPlanIntent(userRequirement)).isTrue();
    }
}
