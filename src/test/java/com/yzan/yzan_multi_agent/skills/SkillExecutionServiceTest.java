package com.yzan.yzan_multi_agent.skills;

import com.yzan.yzan_multi_agent.domain.FurnitureSearchResult;
import com.yzan.yzan_multi_agent.domain.MaterialStoreRecommendation;
import com.yzan.yzan_multi_agent.mcp.FurnitureSearchClient;
import com.yzan.yzan_multi_agent.mcp.MaterialSearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillExecutionServiceTest {

    private final SkillLoader skillLoader = new SkillLoader();
    private final SkillIntentRouter skillIntentRouter = new SkillIntentRouter();

    @Mock
    private MaterialSearchClient materialSearchClient;

    @Mock
    private FurnitureSearchClient furnitureSearchClient;

    private SkillExecutionService skillExecutionService;

    @BeforeEach
    void setUp() {
        this.skillExecutionService = new SkillExecutionService(
                skillLoader,
                skillIntentRouter,
                materialSearchClient,
                furnitureSearchClient
        );
    }

    @Test
    void shouldPrintLoadedSkillsFromResources() {
        List<SkillDefinition> skills = skillLoader.loadAllSkills();

        System.out.println("========== Loaded Skills ==========");
        System.out.println("Skill count = " + skills.size());
        for (SkillDefinition skill : skills) {
            System.out.println("Skill name = " + skill.getSkillName());
            System.out.println("Resource path = " + skill.getResourcePath());
            System.out.println("Content preview = ");
            System.out.println(preview(skill.getContent()));
            System.out.println("-----------------------------------");
        }
    }

    @Test
    void shouldPrintMaterialStoreSkillExecutionResult() {
        MaterialStoreRecommendation store = new MaterialStoreRecommendation();
        store.setMaterialKeyword("防滑地砖");
        store.setStoreName("上海建材城");
        store.setAddress("浦东新区某某路 100 号");
        store.setDistance("1.8km");

        when(materialSearchClient.searchNearestStores("上海", "防滑地砖"))
                .thenReturn(List.of(store));

        SkillExecutionResult result = skillExecutionService.execute(
                "帮我看看上海附近哪里买防滑地砖",
                "上海",
                "防滑地砖",
                null,
                null
        );

        System.out.println("========== Material Skill Result ==========");
        System.out.println("Triggered = " + result.isTriggered());
        System.out.println("Skill name = " + result.getSkillName());
        System.out.println("MCP service = " + result.getMcpService());
        System.out.println("Message = " + result.getMessage());
        System.out.println("Skill prompt preview = ");
        System.out.println(preview(result.getSkillPrompt()));
        System.out.println("Material stores = " + result.getMaterialStoreRecommendations());
        System.out.println("===========================================");
    }

    @Test
    void shouldPrintFurnitureSkillExecutionResult() {
        FurnitureSearchResult furniture = new FurnitureSearchResult();
        furniture.setPlatform("jd");
        furniture.setKeyword("现代简约沙发");
        furniture.setTitle("现代简约布艺沙发");
        furniture.setPrice("2399");
        furniture.setShopName("京东自营");
        furniture.setLink("https://item.jd.com/test");

        when(furnitureSearchClient.searchFurniture("jd", "现代简约沙发"))
                .thenReturn(List.of(furniture));

        SkillExecutionResult result = skillExecutionService.execute(
                "帮我在京东搜几款现代简约沙发",
                null,
                null,
                "jd",
                "现代简约沙发"
        );

        System.out.println("========== Furniture Skill Result ==========");
        System.out.println("Triggered = " + result.isTriggered());
        System.out.println("Skill name = " + result.getSkillName());
        System.out.println("MCP service = " + result.getMcpService());
        System.out.println("Message = " + result.getMessage());
        System.out.println("Skill prompt preview = ");
        System.out.println(preview(result.getSkillPrompt()));
        System.out.println("Furniture results = " + result.getFurnitureSearchResults());
        System.out.println("============================================");
    }

    @Test
    void shouldPrintNoSkillTriggeredResult() {
        SkillExecutionResult result = skillExecutionService.execute(
                "帮我总结一下这个装修方案的优缺点",
                null,
                null,
                null,
                null
        );

        System.out.println("========== No Skill Result ==========");
        System.out.println("Triggered = " + result.isTriggered());
        System.out.println("Message = " + result.getMessage());
        System.out.println("=====================================");
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}
