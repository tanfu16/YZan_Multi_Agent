package com.yzan.yzan_multi_agent.agent;

import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;

import java.util.ArrayList;
import java.util.List;

/**
 * 需求整理员
 * 把用户原始需求整理成结构化需求
 */
public class RequirementAgent {

    public StructuredRequirement execute(UserRequirement userRequirement){
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

    // 分析家庭结构
    private String buildFamilyProfile(List<String> familyMembers) {
        if (familyMembers == null || familyMembers.isEmpty()) {
            return "未知家庭结构";
        }
        return String.join("+", familyMembers);
    }

    // 分析优先级
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

    // 分析约束条件
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


}


