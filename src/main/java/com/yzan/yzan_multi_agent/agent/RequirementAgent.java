package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.RequirementExtractionResult;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 需求整理员
 * 把用户原始需求整理成结构化需求
 * 流程：
 * 用户输入拼上 prompt 模板传给 LLM
 * LLM 返回回答
 * 用 Agent 对应的 Result 类封装回答的信息
 * 整合成标准化输入
 */
@Component
public class RequirementAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public RequirementAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper){
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    // LLM 将用户的原始输入解析成标准化输入
    public StructuredRequirement execute(UserRequirement userRequirement) {
        try {
            String prompt = buildPrompt(userRequirement);
            String response = qwenChatModel.chat(prompt);
            RequirementExtractionResult extractionResult = parseLlmResponse(response);
            return buildStructuredRequirement(userRequirement, extractionResult);
        } catch (Exception e) {
            System.out.println("RequirementAgent: using fallback path");
            e.printStackTrace();
            return fallbackToRuleBased(userRequirement);
        }
    }


    // 构建prompt
    private String buildPrompt(UserRequirement userRequirement) {
        return """
                你是一个装修需求结构化助手。
                你的任务是根据用户输入，提取并整理出固定 JSON。
                
                规则：
                1. 只输出合法 JSON
                2. 不要输出 markdown
                3. 不要输出解释说明
                4. JSON 字段固定为：
                   - familyProfile
                   - stylePreference
                   - priorities
                   - constraints
                
                字段要求：
                - familyProfile: 用简洁中文概括家庭结构
                - stylePreference: 提炼用户风格偏好
                - priorities: 输出数组，表示核心优先级
                - constraints: 输出数组，表示约束条件
                
                用户输入如下：
                houseType: %s
                area: %s
                budget: %s
                familyMembers: %s
                stylePreference: %s
                specialNeeds: %s
                rawDescription: %s
                """.formatted(
                safe(userRequirement.getHouseType()),
                safe(userRequirement.getArea()),
                safe(userRequirement.getBudget()),
                safe(userRequirement.getFamilyMembers()),
                safe(userRequirement.getStylePreference()),
                safe(userRequirement.getSpecialNeeds()),
                safe(userRequirement.getRawDescription())
        );
    }

    // 判断是否为空
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }


    // 模型返回的结果解析成 JSON 对象
    private RequirementExtractionResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, RequirementExtractionResult.class);
    }


    // 从特殊需求和原始描述中提取约束条件
    private List<String> buildConstraints(UserRequirement userRequirement) {
        List<String> constraints = new ArrayList<>();

        if (userRequirement.getSpecialNeeds() != null) {
            for (String need : userRequirement.getSpecialNeeds()) {
                if (need.contains("不要复杂吊顶")) {
                    constraints.add("避免复杂吊顶");
                }
                if (need.contains("孩子")) {
                    constraints.add("避免尖角家具");
                }
                if (need.contains("宠物")) {
                    constraints.add("选择耐脏耐磨材料");
                }
            }
        }

        if (userRequirement.getRawDescription() != null) {
            String raw = userRequirement.getRawDescription();
            if (raw.contains("孩子")) {
                constraints.add("避免尖角家具");
            }
            if (raw.contains("宠物") || raw.contains("猫") || raw.contains("狗")) {
                constraints.add("选择耐脏耐磨材料");
            }
        }

        return constraints;
    }


    // 用户原始输入和模型返回结果合并成标准化输入
    private StructuredRequirement buildStructuredRequirement(
            UserRequirement userRequirement,
            RequirementExtractionResult extractionResult
    ) {
        StructuredRequirement requirement = new StructuredRequirement();

        // 这些字段本身已经结构化，直接透传
        requirement.setHouseType(userRequirement.getHouseType());
        requirement.setArea(userRequirement.getArea());
        requirement.setBudget(userRequirement.getBudget());

        // 这些字段由模型提取
        requirement.setFamilyProfile(extractionResult.getFamilyProfile());
        requirement.setStylePreference(
                extractionResult.getStylePreference() != null
                        ? extractionResult.getStylePreference()
                        : userRequirement.getStylePreference()
        );
        requirement.setPriorities(
                extractionResult.getPriorities() != null
                        ? extractionResult.getPriorities()
                        : List.of("实用")
        );
        requirement.setConstraints(
                extractionResult.getConstraints() != null
                        ? extractionResult.getConstraints()
                        : List.of()
        );

        return requirement;
    }


    // 模型失效策略
    private StructuredRequirement fallbackToRuleBased(UserRequirement userRequirement) {
        StructuredRequirement requirement = new StructuredRequirement();

        requirement.setHouseType(userRequirement.getHouseType());
        requirement.setArea(userRequirement.getArea());
        requirement.setBudget(userRequirement.getBudget());
        requirement.setStylePreference(userRequirement.getStylePreference());

        requirement.setFamilyProfile(buildFamilyProfile(userRequirement.getFamilyMembers()));
        requirement.setPriorities(buildPriorities(userRequirement));
        requirement.setConstraints(buildConstraints(userRequirement));

        return requirement;
    }



    // 模型失效情况下的家庭列表构建
    private String buildFamilyProfile(List<String> familyMembers) {
        if (familyMembers == null || familyMembers.isEmpty()) {
            return "未知家庭结构";
        }
        return String.join("+", familyMembers);
    }


    // 模型失效情况下的优先级提取
    private List<String> buildPriorities(UserRequirement userRequirement) {
        List<String> priorities = new ArrayList<>();

        if (userRequirement.getSpecialNeeds() != null) {
            for (String need : userRequirement.getSpecialNeeds()) {
                if (need.contains("收纳")) {
                    priorities.add("收纳");
                }
                if (need.contains("安全")) {
                    priorities.add("安全");
                }
                if (need.contains("清洁") || need.contains("好打理")) {
                    priorities.add("清洁");
                }
                if (need.contains("通透")) {
                    priorities.add("通透");
                }
            }
        }

        if (priorities.isEmpty()) {
            priorities.add("实用");
        }

        return priorities;
    }
}



