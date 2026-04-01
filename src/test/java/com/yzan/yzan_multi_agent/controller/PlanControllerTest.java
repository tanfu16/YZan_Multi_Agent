package com.yzan.yzan_multi_agent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGenerateDecorationPlanSuccessfully() throws Exception {
        String requestBody = """
                {
                  "houseType": "两室一厅",
                  "area": 89,
                  "budget": 180000,
                  "familyMembers": ["夫妻", "孩子", "宠物"],
                  "stylePreference": "现代原木风",
                  "specialNeeds": ["收纳多", "好打理"],
                  "rawDescription": "预算18万，希望整体温馨一些，适合孩子和宠物活动。"
                }
                """;

        mockMvc.perform(post("/api/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.keyRecommendations").isArray())
                .andExpect(jsonPath("$.conflicts").isArray())
                .andExpect(jsonPath("$.finalSuggestion").isNotEmpty());
    }
}