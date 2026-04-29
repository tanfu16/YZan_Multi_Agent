package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class PlanIntentService {

    private static final List<String> DECORATION_KEYWORDS = List.of(
            "装修", "设计", "方案", "改造", "翻新", "软装", "硬装", "全屋",
            "户型", "客厅", "卧室", "厨房", "卫生间", "阳台", "玄关", "儿童房", "老人房",
            "一室", "二室", "两室", "三室", "四室", "五室", "1室", "2室", "3室", "4室", "5室",
            "预算", "风格", "收纳", "动线", "防滑", "耐磨", "好打理", "宠物", "孩子", "老人",
            "平米", "平方米", "㎡"
    );

    private static final List<String> SMALL_TALK_KEYWORDS = List.of(
            "你好", "您好", "嗨", "hi", "hello", "在吗", "谢谢", "感谢", "早上好", "下午好", "晚上好"
    );

    public boolean isDecorationPlanIntent(UserRequirement userRequirement) {
        if (userRequirement == null) {
            return false;
        }

        if (hasStructuredDecorationFields(userRequirement)) {
            return true;
        }

        String text = normalize(userRequirement.getRawDescription());
        if (text.isBlank()) {
            return false;
        }

        if (isOnlySmallTalk(text)) {
            return false;
        }

        return containsAny(text, DECORATION_KEYWORDS);
    }

    private boolean hasStructuredDecorationFields(UserRequirement userRequirement) {
        return hasText(userRequirement.getHouseType())
                || userRequirement.getArea() != null
                || isPositive(userRequirement.getBudget())
                || hasText(userRequirement.getStylePreference())
                || hasAny(userRequirement.getFamilyMembers())
                || hasAny(userRequirement.getSpecialNeeds());
    }

    private boolean isOnlySmallTalk(String text) {
        String compact = text.replaceAll("[\\s，。,.!！?？~～呀啊呢哈]+", "");
        if (compact.isBlank()) {
            return true;
        }

        for (String keyword : SMALL_TALK_KEYWORDS) {
            if (compact.equals(keyword) || compact.equals(keyword + "呀") || compact.equals(keyword + "啊")) {
                return true;
            }
        }

        return compact.length() <= 6 && containsAny(compact, SMALL_TALK_KEYWORDS);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasAny(List<String> values) {
        return values != null && values.stream().anyMatch(this::hasText);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
