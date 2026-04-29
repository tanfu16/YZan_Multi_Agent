package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.ConversationResponse;
import com.yzan.yzan_multi_agent.domain.RequirementUnderstandingResult;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import com.yzan.yzan_multi_agent.domain.enums.UserIntentType;
import com.yzan.yzan_multi_agent.skills.SkillExecutionResult;
import com.yzan.yzan_multi_agent.skills.SkillInvocationService;
import com.yzan.yzan_multi_agent.skills.SkillIntentRouter;
import com.yzan.yzan_multi_agent.workflow.PlanGenerationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final RequirementUnderstandingService requirementUnderstandingService;
    private final PlanGenerationService planGenerationService;
    private final SkillInvocationService skillInvocationService;
    private final ConversationMemoryService conversationMemoryService;

    public ConversationService(RequirementUnderstandingService requirementUnderstandingService,
                               PlanGenerationService planGenerationService,
                               SkillInvocationService skillInvocationService,
                               ConversationMemoryService conversationMemoryService) {
        this.requirementUnderstandingService = requirementUnderstandingService;
        this.planGenerationService = planGenerationService;
        this.skillInvocationService = skillInvocationService;
        this.conversationMemoryService = conversationMemoryService;
    }

    public ConversationResponse handle(UserRequirement userRequirement) {
        RequirementUnderstandingResult understanding = requirementUnderstandingService.understand(userRequirement);
        ConversationResponse response = new ConversationResponse();
        response.setRawInput(understanding.getRawInput());
        response.setIntentType(understanding.getIntentType());
        response.setUnderstanding(understanding);

        if (understanding.getIntentType() == UserIntentType.GENERAL_CHAT) {
            response.setReply(understanding.getReply());
            conversationMemoryService.recordTurnAndMaybeSummarize(userRequirement, response);
            return response;
        }

        if (understanding.getIntentType() == UserIntentType.SKILL_CALL) {
            SkillExecutionResult skillResult = executeSkill(userRequirement, understanding);
            response.setReply(skillResult.getMessage());
            response.setSkillExecutionResult(skillResult);
            conversationMemoryService.recordTurnAndMaybeSummarize(userRequirement, response);
            return response;
        }

        response.setDecorationPlan(
                planGenerationService.execute(userRequirement, understanding.getStructuredRequirement())
        );
        conversationMemoryService.recordTurnAndMaybeSummarize(userRequirement, response);
        return response;
    }

    private SkillExecutionResult executeSkill(UserRequirement userRequirement,
                                              RequirementUnderstandingResult understanding) {
        String raw = understanding.getRawInput();
        String skillName = understanding.getSkillName();
        if (!SkillIntentRouter.MATERIAL_STORE_SKILL.equals(skillName)
                && !SkillIntentRouter.FURNITURE_SEARCH_SKILL.equals(skillName)) {
            return skillInvocationService.execute(raw, null, null, null, null);
        }
        if (SkillIntentRouter.MATERIAL_STORE_SKILL.equals(skillName)) {
            return skillInvocationService.executeMaterialStoreSkill(
                    raw,
                    inferLocation(raw),
                    inferMaterialKeyword(raw)
            );
        }

        return skillInvocationService.executeFurnitureSearchSkill(
                raw,
                inferPlatform(raw),
                inferFurnitureKeyword(raw)
        );
    }

    private String inferLocation(String text) {
        if (text == null || text.isBlank()) {
            return "上海";
        }
        java.util.regex.Matcher districtMatch = java.util.regex.Pattern
                .compile("(北京|上海|天津|重庆)?[^，。,；;]*?(区|县|镇)")
                .matcher(text);
        if (districtMatch.find()) {
            return districtMatch.group().replaceAll("(什么地方|哪里|能买|帮我看看)", "").trim();
        }

        java.util.regex.Matcher cityMatch = java.util.regex.Pattern
                .compile("(北京|上海|天津|重庆|[^\\s，。,；;]{2,8}市)")
                .matcher(text);
        return cityMatch.find() ? cityMatch.group().trim() : "上海";
    }

    private String inferMaterialKeyword(String text) {
        return findKeyword(text, "防滑地砖", "瓷砖", "地砖", "乳胶漆", "板材", "地板", "灯具", "五金", "建材");
    }

    private String inferPlatform(String text) {
        if (text != null && (text.contains("京东") || text.contains("jd") || text.contains("JD"))) {
            return "jd";
        }
        return "jd";
    }

    private String inferFurnitureKeyword(String text) {
        if (text == null || text.isBlank()) {
            return "沙发";
        }
        String keyword = findKeyword(text, "现代简约沙发", "沙发", "餐桌", "床", "衣柜", "书柜", "椅子", "落地灯");
        if (keyword != null) {
            return keyword;
        }
        return text.replaceAll("帮我|在京东|搜几款|给我几个候选商品|。", "").trim();
    }

    private String findKeyword(String text, String... keywords) {
        if (text == null) {
            return keywords[keywords.length - 1];
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return keywords[keywords.length - 1];
    }
}
