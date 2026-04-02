package com.yzan.yzan_multi_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.ConflictItem;
import com.yzan.yzan_multi_agent.domain.CoordinatorAnalysisResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.PlanOption;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.enums.AgentType;
import com.yzan.yzan_multi_agent.domain.enums.ConflictSeverity;
import com.yzan.yzan_multi_agent.domain.enums.ConflictType;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CoordinatorAgent {

    private final QwenChatModel qwenChatModel;
    private final ObjectMapper objectMapper;

    public CoordinatorAgent(QwenChatModel qwenChatModel, ObjectMapper objectMapper) {
        this.qwenChatModel = qwenChatModel;
        this.objectMapper = objectMapper;
    }

    public DecorationPlan execute(StructuredRequirement requirement, List<AgentResult> results) {
        try {
            String prompt = buildPrompt(requirement, results);
            String response = qwenChatModel.chat(prompt);
            CoordinatorAnalysisResult analysisResult = parseLlmResponse(response);
            return buildDecorationPlan(analysisResult);
        } catch (Exception e) {
            System.err.println("CoordinatorAgent execute failed: " + e.getMessage());
            return fallbackPlan(requirement, results);
        }
    }

    private String buildPrompt(StructuredRequirement requirement, List<AgentResult> results) {
        return """
            你是一个装修方案协调专家。
            你的任务是根据用户的结构化装修需求，以及多个专业 agent 的分析结果，
            输出最终协调后的装修方案，并严格返回合法 JSON。

            规则：
            1. 只输出合法 JSON
            2. 不要输出 markdown
            3. 不要输出解释说明
            4. 所有字段都必须返回，不能缺失
            5. 如果没有明显冲突，也要返回 conflicts 数组，可以为空数组 []
            6. alternativeOptions 至少返回 2 个备选方案
            7. chosenDirection 必须是以下值之一：
               - BALANCED
               - PRIORITIZE_SAFETY
               - PRIORITIZE_STORAGE
               - PRIORITIZE_BUDGET
               - PRIORITIZE_STYLE
               - PRIORITIZE_OPENNESS

            JSON 字段固定为：
            - summary
            - conflicts
            - primaryOption
            - alternativeOptions
            - decisionReason

            字段要求：

            1. summary
            - 用一句话总结整体协调结果

            2. conflicts
            - 类型：数组
            - 每个冲突对象必须包含以下字段：
              - topic
              - conflictType
              - severity
              - relatedAgents
              - description
              - tradeOff
              - chosenDirection
              - resolution

            conflicts 中字段的具体要求：
            - topic: 中文冲突主题
            - conflictType: 必须是以下值之一：
              - LAYOUT_VS_SAFETY
              - LAYOUT_VS_STORAGE
              - BUDGET_VS_STORAGE
              - BUDGET_VS_STYLE
            - severity: 必须是以下值之一：
              - LOW
              - MEDIUM
              - HIGH
            - relatedAgents: 字符串数组，例如 ["LAYOUT", "SAFETY"]
            - description: 说明冲突是如何产生的
            - tradeOff: 说明这个冲突真正的取舍点是什么
            - chosenDirection: 说明最终更偏向哪一侧
            - resolution: 给出最终协调建议

            3. primaryOption
            - 类型：对象
            - 必须包含以下字段：
              - name
              - positioning
              - recommendations
              - advantages
              - disadvantages
              - applicableCrowd

            primaryOption 字段要求：
            - name: 主方案名称
            - positioning: 一句话说明方案定位
            - recommendations: 3 到 6 条核心建议
            - advantages: 2 到 4 条优点
            - disadvantages: 1 到 3 条缺点或代价
            - applicableCrowd: 一句话说明适用人群

            4. alternativeOptions
            - 类型：数组
            - 至少返回 2 个备选方案
            - 每个元素结构与 primaryOption 完全相同

            5. decisionReason
            - 用一句话说明为什么选择 primaryOption 作为主方案
            - 必须体现用户优先级、冲突权衡或落地可行性

            你不能只做简单汇总，必须显式处理“冲突与取舍”。
            重点关注：
            - 空间通透感 vs 收纳能力
            - 预算控制 vs 材料升级
            - 视觉效果 vs 家庭安全
            - 美观设计 vs 易清洁和低维护

            用户结构化需求如下：
            houseType: %s
            area: %s
            budget: %s
            familyProfile: %s
            stylePreference: %s
            priorities: %s
            constraints: %s

            专业 agent 结果如下：
            %s
            """.formatted(
                safe(requirement.getHouseType()),
                safe(requirement.getArea()),
                safe(requirement.getBudget()),
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints()),
                formatAgentResults(results)
        );
    }


    private CoordinatorAnalysisResult parseLlmResponse(String response) throws Exception {
        return objectMapper.readValue(response, CoordinatorAnalysisResult.class);
    }

    private DecorationPlan buildDecorationPlan(CoordinatorAnalysisResult analysisResult) {
        DecorationPlan plan = new DecorationPlan();
        plan.setSummary(
                analysisResult.getSummary() != null
                        ? analysisResult.getSummary()
                        : "系统已完成多 agent 协调，并生成主方案与备选方案。"
        );
        plan.setConflicts(
                analysisResult.getConflicts() != null
                        ? analysisResult.getConflicts()
                        : List.of()
        );
        plan.setPrimaryOption(
                analysisResult.getPrimaryOption() != null
                        ? analysisResult.getPrimaryOption()
                        : defaultPrimaryOption()
        );
        plan.setAlternativeOptions(
                analysisResult.getAlternativeOptions() != null
                        ? analysisResult.getAlternativeOptions()
                        : List.of()
        );
        plan.setDecisionReason(
                analysisResult.getDecisionReason() != null
                        ? analysisResult.getDecisionReason()
                        : "当前主方案更适合在预算、收纳、空间体验和安全之间取得平衡。"
        );
        return plan;
    }

    private DecorationPlan fallbackPlan(StructuredRequirement requirement, List<AgentResult> results) {
        DecorationPlan plan = new DecorationPlan();

        List<ConflictItem> conflicts = detectConflicts(requirement, results);
        PlanOption primaryOption = buildPrimaryOption(requirement, results, conflicts);
        List<PlanOption> alternativeOptions = buildAlternativeOptions(results, conflicts);

        plan.setSummary(
                conflicts.isEmpty()
                        ? "当前多 agent 结果整体一致，推荐采用平衡型主方案。"
                        : "当前需求存在多维度取舍，系统已生成主方案和备选方案供比较。"
        );
        plan.setConflicts(conflicts);
        plan.setPrimaryOption(primaryOption);
        plan.setAlternativeOptions(alternativeOptions);
        plan.setDecisionReason(buildDecisionReason(requirement, conflicts));

        return plan;
    }

    private PlanOption buildPrimaryOption(StructuredRequirement requirement, List<AgentResult> results, List<ConflictItem> conflicts) {
        String primaryDirection = resolvePrimaryDirection(conflicts);

        return switch (primaryDirection) {
            case "PRIORITIZE_SAFETY" -> buildSafetyPrimaryOption(results);
            case "PRIORITIZE_BUDGET" -> buildBudgetPrimaryOption(results);
            case "PRIORITIZE_STORAGE" -> buildStoragePrimaryOption(results);
            case "PRIORITIZE_STYLE" -> buildStylePrimaryOption(results);
            default -> buildBalancedPrimaryOption(results, conflicts);
        };
    }


    private List<PlanOption> buildAlternativeOptions(List<AgentResult> results, List<ConflictItem> conflicts) {
        List<PlanOption> options = new ArrayList<>();
        String primaryDirection = resolvePrimaryDirection(conflicts);

        if (!"PRIORITIZE_BUDGET".equals(primaryDirection)) {
            options.add(buildBudgetAlternativeOption(results));
        }

        if (!"PRIORITIZE_STORAGE".equals(primaryDirection)) {
            options.add(buildStorageAlternativeOption(results));
        }

        if (!"PRIORITIZE_SAFETY".equals(primaryDirection)) {
            options.add(buildSafetyAlternativeOption(results));
        }

        if (!"PRIORITIZE_STYLE".equals(primaryDirection)) {
            options.add(buildStyleAlternativeOption(results));
        }

        if (!"BALANCED".equals(primaryDirection)) {
            options.add(buildBalancedAlternativeOption(results, conflicts));
        }

        return options.stream().limit(3).toList();
    }


    private String buildDecisionReason(StructuredRequirement requirement, List<ConflictItem> conflicts) {
        String primaryDirection = resolvePrimaryDirection(conflicts);

        return switch (primaryDirection) {
            case "PRIORITIZE_SAFETY" ->
                    "由于家庭结构对安全更敏感，且用户对安全诉求明确，主方案优先降低日常使用风险，并在通透感和视觉效果上做适度折中。";
            case "PRIORITIZE_BUDGET" ->
                    "由于当前预算约束较强，主方案优先保证核心功能和整体可落地性，避免在风格升级和高成本定制上产生明显超支。";
            case "PRIORITIZE_STORAGE" ->
                    "由于用户明确强调收纳需求，主方案优先保障高价值收纳区域的完整性，并通过局部留白控制空间压迫感。";
            case "PRIORITIZE_STYLE" ->
                    "由于用户更重视风格表达和空间审美，主方案优先保证核心视觉区域的设计统一性，同时对预算做适度集中投入。";
            default ->
                    "由于当前需求涉及预算、收纳、空间体验和安全等多维目标，主方案选择了更容易综合落地的平衡路线。";
        };
    }


    private PlanOption defaultPrimaryOption() {
        PlanOption option = new PlanOption();
        option.setName("默认主方案");
        option.setPositioning("兼顾实用性与舒适度的综合方案");
        option.setRecommendations(List.of("建议优先采用兼顾预算、空间和家庭适配性的综合装修路线"));
        option.setAdvantages(List.of("整体更稳妥"));
        option.setDisadvantages(List.of("单项能力不一定最突出"));
        option.setApplicableCrowd("适合大多数普通家庭");
        return option;
    }

    private List<ConflictItem> detectConflicts(StructuredRequirement requirement, List<AgentResult> results) {
        List<ConflictItem> conflicts = new ArrayList<>();

        boolean hasLayout = false;
        boolean hasSafety = false;
        boolean hasStorage = false;
        boolean hasBudget = false;

        for (AgentResult result : results) {
            if (result.getAgentType() == AgentType.LAYOUT) {
                hasLayout = true;
            }
            if (result.getAgentType() == AgentType.SAFETY) {
                hasSafety = true;
            }
            if (result.getAgentType() == AgentType.STORAGE) {
                hasStorage = true;
            }
            if (result.getAgentType() == AgentType.BUDGET) {
                hasBudget = true;
            }
        }

        if (hasLayout && hasSafety) {
            ConflictItem item = new ConflictItem();
            item.setTopic("空间通透感与家庭安全");
            item.setConflictType(ConflictType.LAYOUT_VS_SAFETY);

            boolean highSafetySensitiveFamily = isHighSafetySensitiveFamily(requirement);
            boolean safetyPriority = isSafetyPriority(requirement);

            if (highSafetySensitiveFamily || safetyPriority) {
                item.setSeverity(ConflictSeverity.HIGH);
            } else {
                item.setSeverity(ConflictSeverity.MEDIUM);
            }

            item.setRelatedAgents(List.of("LAYOUT", "SAFETY"));

            if (highSafetySensitiveFamily) {
                item.setDescription("开放式布局更强调通透和视觉轻盈，但当前家庭包含孩子、老人或宠物，日常活动中的磕碰和滑倒风险需要被更严格控制。");
            } else {
                item.setDescription("开放式布局更强调通透和视觉轻盈，但更开放的动线和家具布局也会提高家庭日常使用中的安全控制要求。");
            }

            item.setTradeOff("更开放的空间体验通常意味着要付出更高的安全防护成本，并在家具造型、动线和材料选择上做出限制。");

            if (safetyPriority) {
                item.setChosenDirection("PRIORITIZE_SAFETY");
                item.setResolution("建议优先保证家庭成员长期使用安全，保留适度通透感，并通过圆角家具、防滑材料、封闭式低位收纳和更稳妥的动线设计降低风险。");
            } else {
                item.setChosenDirection("BALANCED");
                item.setResolution("建议保留整体通透感，同时通过圆角家具、防滑材料和更稳妥的动线设计降低风险，在开放感和安全性之间取得平衡。");
            }

            conflicts.add(item);
        }


        if (hasLayout && hasStorage) {
            ConflictItem item = new ConflictItem();
            item.setTopic("空间通透感与收纳能力");
            item.setConflictType(ConflictType.LAYOUT_VS_STORAGE);

            boolean storagePriority = isStoragePriority(requirement);

            if (storagePriority) {
                item.setSeverity(ConflictSeverity.HIGH);
            } else {
                item.setSeverity(ConflictSeverity.MEDIUM);
            }

            item.setRelatedAgents(List.of("LAYOUT", "STORAGE"));
            item.setDescription("增加柜体和隐藏式储物设计能显著提升收纳能力，但柜体密度过高也会压缩公共区域的空间感和视觉轻盈度。");
            item.setTradeOff("收纳最大化通常意味着要牺牲一部分通透感、开放感和视觉简洁度。");

            if (storagePriority) {
                item.setChosenDirection("PRIORITIZE_STORAGE");
                item.setResolution("建议优先保障高价值收纳区域，例如玄关、卧室和餐边柜，并通过局部留白、浅色柜体和减少开放格数量来降低空间压迫感。");
            } else {
                item.setChosenDirection("BALANCED");
                item.setResolution("建议优先在关键区域补足基础收纳，同时控制公共区域柜体密度，在通透感和储物能力之间取得平衡。");
            }

            conflicts.add(item);
        }


        if (hasBudget && hasStorage) {
            ConflictItem item = new ConflictItem();
            item.setTopic("预算控制与收纳升级");
            item.setConflictType(ConflictType.BUDGET_VS_STORAGE);

            boolean budgetSensitive = isBudgetSensitive(requirement);
            boolean storagePriority = isStoragePriority(requirement);

            if (budgetSensitive && storagePriority) {
                item.setSeverity(ConflictSeverity.HIGH);
            } else {
                item.setSeverity(ConflictSeverity.MEDIUM);
            }

            item.setRelatedAgents(List.of("BUDGET", "STORAGE"));
            item.setDescription("定制柜体、分区收纳和隐藏式储物通常能提升长期整理效率，但也会明显增加前期投入，容易和预算控制目标发生冲突。");
            item.setTradeOff("更完整的收纳体系通常意味着更高的定制成本，而严格控预算又会限制收纳升级空间。");

            if (budgetSensitive) {
                item.setChosenDirection("PRIORITIZE_BUDGET");
                item.setResolution("建议优先保证基础收纳能力，把高成本定制收纳集中在玄关、主卧和餐边柜等高频区域，非核心空间采用后续逐步补充的方式控制预算。");
            } else if (storagePriority) {
                item.setChosenDirection("PRIORITIZE_STORAGE");
                item.setResolution("建议优先保证关键生活场景的完整收纳体系，并通过简化装饰性材料、压缩非必要造型预算来支持收纳升级。");
            } else {
                item.setChosenDirection("BALANCED");
                item.setResolution("建议优先满足基础收纳需求，控制非核心区域的定制强度，在预算和收纳提升之间保持平衡。");
            }

            conflicts.add(item);
        }

        if (hasBudget) {
            ConflictItem item = new ConflictItem();
            item.setTopic("预算控制与风格升级");
            item.setConflictType(ConflictType.BUDGET_VS_STYLE);

            boolean budgetSensitive = isBudgetSensitive(requirement);
            boolean stylePriority = isStylePriority(requirement);

            if (budgetSensitive && stylePriority) {
                item.setSeverity(ConflictSeverity.HIGH);
            } else if (stylePriority) {
                item.setSeverity(ConflictSeverity.MEDIUM);
            } else {
                item.setSeverity(ConflictSeverity.LOW);
            }

            item.setRelatedAgents(List.of("BUDGET"));
            item.setDescription("更强的风格表达通常需要更高质量的材料、统一的设计语言和更多细节处理，这些都会推高整体装修预算。");
            item.setTradeOff("追求更高的风格完成度通常意味着更高的材料和施工成本，而严格控预算会压缩风格升级空间。");

            if (budgetSensitive) {
                item.setChosenDirection("PRIORITIZE_BUDGET");
                item.setResolution("建议优先保证整体预算可控，在硬装和大件材料上保持克制，把风格表达集中到局部软装、灯光和关键视觉区，降低整体投入。");
            } else if (stylePriority) {
                item.setChosenDirection("PRIORITIZE_STYLE");
                item.setResolution("建议优先保证核心空间的风格统一性和材料质感，同时压缩非重点区域预算，把资金集中投入到最能体现风格的部分。");
            } else {
                item.setChosenDirection("BALANCED");
                item.setResolution("建议采用适度风格化路线，在控制整体预算的前提下，通过局部材质和色彩搭配提升空间审美。");
            }

            conflicts.add(item);
        }


        return conflicts;
    }

    private List<String> collectRecommendations(List<AgentResult> results) {
        List<String> recommendations = new ArrayList<>();
        for (AgentResult result : results) {
            if (result.getRecommendations() != null) {
                recommendations.addAll(result.getRecommendations());
            }
        }
        return recommendations;
    }

    private List<String> limitRecommendations(List<String> recommendations, int limit) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of("建议优先采用兼顾实用性与舒适度的综合装修方案");
        }
        return recommendations.stream().limit(limit).toList();
    }

    private List<String> extractRecommendationsByAgent(List<AgentResult> results, AgentType agentType) {
        for (AgentResult result : results) {
            if (result.getAgentType() == agentType
                    && result.getRecommendations() != null
                    && !result.getRecommendations().isEmpty()) {
                return result.getRecommendations();
            }
        }
        return List.of("当前未获取到该方向的明确建议");
    }

    private String formatAgentResults(List<AgentResult> results) {
        StringBuilder builder = new StringBuilder();

        for (AgentResult result : results) {
            builder.append("agentType: ").append(result.getAgentType()).append("\n");
            builder.append("status: ").append(result.getAgentExecutionStatus()).append("\n");
            builder.append("recommendations: ").append(result.getRecommendations()).append("\n");
            builder.append("risks: ").append(result.getRisks()).append("\n");
            builder.append("summary: ").append(result.getSummary()).append("\n\n");
        }

        return builder.toString();
    }


    // 判断用户需求中是不是安全优先
    private boolean isSafetyPriority(StructuredRequirement requirement) {
        if (requirement == null || requirement.getPriorities() == null) {
            return false;
        }
        return requirement.getPriorities().stream()
                .anyMatch(priority -> containsKeyword(priority, "安全"));
    }

    // 判断用户需求中是不是收纳优先
    private boolean isStoragePriority(StructuredRequirement requirement) {
        if (requirement == null || requirement.getPriorities() == null) {
            return false;
        }
        return requirement.getPriorities().stream()
                .anyMatch(priority -> containsKeyword(priority, "收纳"));
    }

    // 判断用户需求中是不是预算优先
    private boolean isBudgetPriority(StructuredRequirement requirement) {
        if (requirement == null || requirement.getPriorities() == null) {
            return false;
        }
        return requirement.getPriorities().stream()
                .anyMatch(priority -> containsKeyword(priority, "预算") || containsKeyword(priority, "成本"));
    }

    // 判断用户需求中是不是风格优先
    private boolean isStylePriority(StructuredRequirement requirement) {
        if (requirement == null || requirement.getPriorities() == null) {
            return false;
        }
        return requirement.getPriorities().stream()
                .anyMatch(priority -> containsKeyword(priority, "风格")
                        || containsKeyword(priority, "美观")
                        || containsKeyword(priority, "颜值"));
    }

    // 判断用户需求中是不是安全优先（强化）
    private boolean isHighSafetySensitiveFamily(StructuredRequirement requirement) {
        if (requirement == null || requirement.getFamilyProfile() == null) {
            return false;
        }
        String familyProfile = requirement.getFamilyProfile();
        return containsKeyword(familyProfile, "孩子")
                || containsKeyword(familyProfile, "儿童")
                || containsKeyword(familyProfile, "老人")
                || containsKeyword(familyProfile, "宠物");
    }

    // 判断用户需求中是不是预算优先（强化）
    private boolean isBudgetSensitive(StructuredRequirement requirement) {
        if (isBudgetPriority(requirement)) {
            return true;
        }

        if (requirement == null || requirement.getBudget() == null || requirement.getArea() == null || requirement.getArea() <= 0) {
            return false;
        }

        return requirement.getBudget()
                .divide(java.math.BigDecimal.valueOf(requirement.getArea()), 0, java.math.RoundingMode.HALF_UP)
                .compareTo(new java.math.BigDecimal("1500")) < 0;
    }

    private boolean containsKeyword(String text, String keyword) {
        return text != null && text.contains(keyword);
    }


    // 选择主导方向
    private String resolvePrimaryDirection(List<ConflictItem> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return "BALANCED";
        }

        boolean hasSafety = conflicts.stream()
                .anyMatch(conflict -> "PRIORITIZE_SAFETY".equals(conflict.getChosenDirection()));

        if (hasSafety) {
            return "PRIORITIZE_SAFETY";
        }

        boolean hasBudget = conflicts.stream()
                .anyMatch(conflict -> "PRIORITIZE_BUDGET".equals(conflict.getChosenDirection()));

        if (hasBudget) {
            return "PRIORITIZE_BUDGET";
        }

        boolean hasStorage = conflicts.stream()
                .anyMatch(conflict -> "PRIORITIZE_STORAGE".equals(conflict.getChosenDirection()));

        if (hasStorage) {
            return "PRIORITIZE_STORAGE";
        }

        boolean hasStyle = conflicts.stream()
                .anyMatch(conflict -> "PRIORITIZE_STYLE".equals(conflict.getChosenDirection()));

        if (hasStyle) {
            return "PRIORITIZE_STYLE";
        }

        return "BALANCED";
    }

    // 平衡型主方案
    private PlanOption buildBalancedPrimaryOption(List<AgentResult> results, List<ConflictItem> conflicts) {
        PlanOption option = new PlanOption();
        option.setName("平衡型主方案");
        option.setPositioning("优先兼顾预算、收纳、空间通透感和家庭安全");
        option.setRecommendations(limitRecommendations(collectRecommendations(results), 6));
        option.setAdvantages(List.of(
                "整体更均衡，适合多数家庭稳定落地",
                "能兼顾长期居住体验与可执行性",
                "对预算和空间压迫感有一定控制"
        ));
        option.setDisadvantages(
                conflicts.isEmpty()
                        ? List.of("在单一目标上不如极致取向方案突出")
                        : List.of("需要在收纳、预算和空间体验之间接受部分妥协")
        );
        option.setApplicableCrowd("适合希望兼顾实用性、美观度和家庭适配性的家庭");
        return option;
    }

    // 安全优先主方案
    private PlanOption buildSafetyPrimaryOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("安全优先主方案");
        option.setPositioning("优先降低家庭成员日常使用中的磕碰、滑倒和维护风险");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.SAFETY));
        option.setAdvantages(List.of(
                "更适合有孩子、老人或宠物的家庭",
                "长期使用风险更低",
                "更利于日常稳定居住"
        ));
        option.setDisadvantages(List.of(
                "部分开放式和强视觉设计需要让步",
                "空间通透感可能不是最极致"
        ));
        option.setApplicableCrowd("适合对家庭安全性要求高的用户");
        return option;
    }

    // 预算优先主方案
    private PlanOption buildBudgetPrimaryOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("预算优先主方案");
        option.setPositioning("优先控制总体成本，保证核心功能区稳定落地");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.BUDGET));
        option.setAdvantages(List.of(
                "预算压力更小",
                "更容易控制超支风险",
                "适合尽快落地实施"
        ));
        option.setDisadvantages(List.of(
                "材料升级和个性化设计空间有限",
                "部分收纳或风格提升需要分阶段完成"
        ));
        option.setApplicableCrowd("适合预算敏感、优先追求可落地性的家庭");
        return option;
    }

    // 收纳优先主方案
    private PlanOption buildStoragePrimaryOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("收纳优先主方案");
        option.setPositioning("优先提升储物能力、整理便利性和长期居住秩序");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.STORAGE));
        option.setAdvantages(List.of(
                "更适合物品较多或长期居住的家庭",
                "更容易维持空间整洁",
                "高频生活区域更实用"
        ));
        option.setDisadvantages(List.of(
                "公共区域通透感可能有所下降",
                "柜体增多后预算可能抬升"
        ));
        option.setApplicableCrowd("适合小户型或收纳需求明显更强的家庭");
        return option;
    }


    // 风格优先主方案
    private PlanOption buildStylePrimaryOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("风格优先主方案");
        option.setPositioning("优先保证空间风格统一性和视觉表现力");
        option.setRecommendations(limitRecommendations(collectRecommendations(results), 5));
        option.setAdvantages(List.of(
                "整体视觉完成度更高",
                "更容易形成明确设计风格",
                "空间表现更有记忆点"
        ));
        option.setDisadvantages(List.of(
                "预算压力可能更大",
                "部分实用性或低维护诉求需要让步"
        ));
        option.setApplicableCrowd("适合更重视审美表达和空间风格体验的用户");
        return option;
    }


    // 预算优先方案
    private PlanOption buildBudgetAlternativeOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("预算优先方案");
        option.setPositioning("优先控制总体成本，保证核心功能落地");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.BUDGET));
        option.setAdvantages(List.of(
                "预算压力更小",
                "更适合成本敏感型家庭"
        ));
        option.setDisadvantages(List.of(
                "材料升级和视觉效果空间有限",
                "部分个性化设计需要让步"
        ));
        option.setApplicableCrowd("适合预算有限、优先追求可落地性的家庭");
        return option;
    }


    // 收纳优先方案
    private PlanOption buildStorageAlternativeOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("收纳优先方案");
        option.setPositioning("优先提升储物能力和长期整理便利性");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.STORAGE));
        option.setAdvantages(List.of(
                "更适合物品较多的家庭",
                "长期生活秩序更容易维持"
        ));
        option.setDisadvantages(List.of(
                "可能压缩部分通透感",
                "柜体增多后预算可能提升"
        ));
        option.setApplicableCrowd("适合小户型或收纳需求强的家庭");
        return option;
    }

    // 安全优先方案
    private PlanOption buildSafetyAlternativeOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("安全优先方案");
        option.setPositioning("优先降低日常磕碰、滑倒和维护风险");
        option.setRecommendations(extractRecommendationsByAgent(results, AgentType.SAFETY));
        option.setAdvantages(List.of(
                "更适合有孩子、老人或宠物的家庭",
                "长期使用风险更低"
        ));
        option.setDisadvantages(List.of(
                "部分开放式或强设计感方案需要让步",
                "材料和造型选择会受到限制"
        ));
        option.setApplicableCrowd("适合家庭安全性要求较高的用户");
        return option;
    }

    // 风格优先方案
    private PlanOption buildStyleAlternativeOption(List<AgentResult> results) {
        PlanOption option = new PlanOption();
        option.setName("风格优先方案");
        option.setPositioning("优先保证空间风格统一性和视觉表现力");
        option.setRecommendations(limitRecommendations(collectRecommendations(results), 5));
        option.setAdvantages(List.of(
                "整体视觉完成度更高",
                "更容易形成明确设计风格"
        ));
        option.setDisadvantages(List.of(
                "预算压力可能更大",
                "部分实用性诉求需要让步"
        ));
        option.setApplicableCrowd("适合更重视审美表达和空间氛围的用户");
        return option;
    }


    // 平衡型方案
    private PlanOption buildBalancedAlternativeOption(List<AgentResult> results, List<ConflictItem> conflicts) {
        PlanOption option = new PlanOption();
        option.setName("平衡型方案");
        option.setPositioning("在预算、收纳、空间体验和安全之间保持综合折中");
        option.setRecommendations(limitRecommendations(collectRecommendations(results), 6));
        option.setAdvantages(List.of(
                "整体更均衡",
                "适用范围更广"
        ));
        option.setDisadvantages(
                conflicts == null || conflicts.isEmpty()
                        ? List.of("单项表现不一定最突出")
                        : List.of("需要在多目标之间接受适度妥协")
        );
        option.setApplicableCrowd("适合没有单一极强偏好、希望整体稳妥落地的家庭");
        return option;
    }


    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
