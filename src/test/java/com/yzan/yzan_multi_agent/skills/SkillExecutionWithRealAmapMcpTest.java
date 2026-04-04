package com.yzan.yzan_multi_agent.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.config.AmapMcpConfig;
import com.yzan.yzan_multi_agent.mcp.AmapMcpMaterialSearchClient;
import com.yzan.yzan_multi_agent.mcp.FurnitureSearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = SkillExecutionWithRealAmapMcpTest.TestConfig.class)
@ActiveProfiles("local")
class SkillExecutionWithRealAmapMcpTest {

    @jakarta.annotation.Resource
    private SkillExecutionService skillExecutionService;

    @Test
    void shouldPrintSkillExecutionResultWithRealAmapMcp() {
        String userRequest = "帮我看看上海附近哪里买防滑地砖";
        String location = "上海";
        String materialKeyword = "防滑地砖";

        SkillExecutionResult result = skillExecutionService.execute(
                userRequest,
                location,
                materialKeyword,
                null,
                null
        );

        System.out.println("========== Skill + Real AMap MCP Result ==========");
        System.out.println("User request = " + userRequest);
        System.out.println("Triggered = " + result.isTriggered());
        System.out.println("Skill name = " + result.getSkillName());
        System.out.println("MCP service = " + result.getMcpService());
        System.out.println("Message = " + result.getMessage());
        System.out.println("Skill prompt preview = ");
        System.out.println(preview(result.getSkillPrompt()));
        System.out.println("Material store count = "
                + (result.getMaterialStoreRecommendations() == null ? 0 : result.getMaterialStoreRecommendations().size()));
        System.out.println("Material stores = " + result.getMaterialStoreRecommendations());
        System.out.println("==================================================");
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    @TestConfiguration
    @Import({
            AmapMcpConfig.class,
            SkillLoader.class,
            SkillIntentRouter.class,
            SkillExecutionService.class,
            AmapMcpMaterialSearchClient.class
    })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        FurnitureSearchClient furnitureSearchClient() {
            return (platform, keyword) -> List.of();
        }
    }
}
