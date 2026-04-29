package com.yzan.yzan_multi_agent.controller;

import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.workflow.PlanGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanControllerTest {

    private MockMvc mockMvc;

    private FakePlanGenerationService planGenerationService;

    @BeforeEach
    void setUp() {
        planGenerationService = new FakePlanGenerationService();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlanController(planGenerationService))
                .build();
    }

    @Test
    void shouldPrintDecorationPlanResponse() throws Exception {
        DecorationPlan plan = new DecorationPlan();
        plan.setSummary("测试方案摘要");
        planGenerationService.plan = plan;

        String requestBody = """
                {
                  \"houseType\": \"两室一厅\",
                  \"area\": 89,
                  \"budget\": 180000,
                  \"familyMembers\": [\"夫妻\", \"孩子\", \"宠物\"],
                  \"stylePreference\": \"现代原木风\",
                  \"specialNeeds\": [\"收纳多\", \"好打理\"],
                  \"rawDescription\": \"预算18万，希望整体温馨一些，适合孩子和宠物活动。\"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("========== PlanController Response ==========");
        System.out.println("HTTP Status = " + result.getResponse().getStatus());
        System.out.println("ContentType = " + result.getResponse().getContentType());
        System.out.println("ResponseBody = ");
        System.out.println(result.getResponse().getContentAsString());
        System.out.println("============================================");

        assertThat(planGenerationService.callCount).isEqualTo(1);
    }

    @Test
    void shouldForwardAnyInputToPlanGenerationWithoutIntentValidation() throws Exception {
        DecorationPlan plan = new DecorationPlan();
        plan.setSummary("通用方案摘要");
        planGenerationService.plan = plan;

        String requestBody = """
                {
                  \"rawDescription\": \"你好\"
                }
                """;

        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        assertThat(planGenerationService.callCount).isEqualTo(1);
    }

    private static class FakePlanGenerationService implements PlanGenerationService {

        private DecorationPlan plan;

        private int callCount;

        @Override
        public DecorationPlan execute(UserRequirement userRequirement) {
            callCount++;
            return plan;
        }
    }
}
