package com.yzan.yzan_multi_agent.skills;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class SkillIntentRouter {

    public static final String MATERIAL_STORE_SKILL = "material-store-search-skill";
    public static final String FURNITURE_SEARCH_SKILL = "furniture-search-skill";

    public Optional<String> route(String userRequest) {
        if (userRequest == null || userRequest.isBlank()) {
            return Optional.empty();
        }

        String normalized = userRequest.toLowerCase(Locale.ROOT);

        if (isMaterialStoreIntent(normalized)) {
            return Optional.of(MATERIAL_STORE_SKILL);
        }

        if (isFurnitureSearchIntent(normalized)) {
            return Optional.of(FURNITURE_SEARCH_SKILL);
        }

        return Optional.empty();
    }

    private boolean isMaterialStoreIntent(String text) {
        return containsAny(text,
                "哪里买", "附近", "门店", "购买地址", "建材市场", "材料店",
                "地砖", "瓷砖", "乳胶漆", "板材", "五金", "地板", "灯具");
    }

    private boolean isFurnitureSearchIntent(String text) {
        return containsAny(text,
                "搜", "搜索", "找几款", "候选商品", "电商", "京东", "淘宝",
                "沙发", "餐桌", "床", "衣柜", "书柜", "椅子", "落地灯");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
