package com.yzan.yzan_multi_agent.skills;

import com.yzan.yzan_multi_agent.domain.FurnitureSearchResult;
import com.yzan.yzan_multi_agent.domain.MaterialStoreRecommendation;
import com.yzan.yzan_multi_agent.mcp.FurnitureSearchClient;
import com.yzan.yzan_multi_agent.mcp.MaterialSearchClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillExecutionService implements SkillInvocationService {

    private final SkillLoader skillLoader;
    private final SkillIntentRouter skillIntentRouter;
    private final MaterialSearchClient materialSearchClient;
    private final FurnitureSearchClient furnitureSearchClient;

    public SkillExecutionService(SkillLoader skillLoader,
                                 SkillIntentRouter skillIntentRouter,
                                 MaterialSearchClient materialSearchClient,
                                 FurnitureSearchClient furnitureSearchClient) {
        this.skillLoader = skillLoader;
        this.skillIntentRouter = skillIntentRouter;
        this.materialSearchClient = materialSearchClient;
        this.furnitureSearchClient = furnitureSearchClient;
    }

    public SkillExecutionResult execute(String userRequest,
                                        String location,
                                        String materialKeyword,
                                        String platform,
                                        String furnitureKeyword) {
        return skillIntentRouter.route(userRequest)
                .map(skillName -> executeSelectedSkill(skillName, userRequest, location, materialKeyword, platform, furnitureKeyword))
                .orElseGet(() -> buildNoSkillResult(userRequest));
    }

    public SkillExecutionResult executeMaterialStoreSkill(String userRequest,
                                                          String location,
                                                          String materialKeyword) {
        return executeSelectedSkill(
                SkillIntentRouter.MATERIAL_STORE_SKILL,
                userRequest,
                location,
                materialKeyword,
                null,
                null
        );
    }

    public SkillExecutionResult executeFurnitureSearchSkill(String userRequest,
                                                            String platform,
                                                            String furnitureKeyword) {
        return executeSelectedSkill(
                SkillIntentRouter.FURNITURE_SEARCH_SKILL,
                userRequest,
                null,
                null,
                platform,
                furnitureKeyword
        );
    }

    private SkillExecutionResult executeSelectedSkill(String skillName,
                                                      String userRequest,
                                                      String location,
                                                      String materialKeyword,
                                                      String platform,
                                                      String furnitureKeyword) {
        SkillDefinition skill = skillLoader.loadSkill(skillName)
                .orElseThrow(() -> new IllegalStateException("Skill document not found: " + skillName));

        SkillExecutionResult result = new SkillExecutionResult();
        result.setTriggered(true);
        result.setUserRequest(userRequest);
        result.setSkillName(skill.getSkillName());
        result.setSkillPrompt(skill.getContent());

        if (SkillIntentRouter.MATERIAL_STORE_SKILL.equals(skillName)) {
            List<MaterialStoreRecommendation> stores =
                    materialSearchClient.searchNearestStores(location, materialKeyword);
            result.setMcpService("AMap MCP");
            result.setMessage("Triggered material store search skill.");
            result.setMaterialStoreRecommendations(stores);
            return result;
        }

        List<FurnitureSearchResult> furniture =
                furnitureSearchClient.searchFurniture(platform, furnitureKeyword);
        result.setMcpService("Playwright MCP");
        result.setMessage("Triggered furniture search skill.");
        result.setFurnitureSearchResults(furniture);
        return result;
    }

    private SkillExecutionResult buildNoSkillResult(String userRequest) {
        SkillExecutionResult result = new SkillExecutionResult();
        result.setTriggered(false);
        result.setUserRequest(userRequest);
        result.setMessage("No matching skill was triggered for the current request.");
        return result;
    }
}
