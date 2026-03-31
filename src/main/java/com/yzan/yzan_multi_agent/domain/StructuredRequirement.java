package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结构化输入
 */
@Data
public class StructuredRequirement {

    private String houseType;

    private Integer area;

    private BigDecimal budget;

    private String familyProfile; // 系统整理后的家庭画像

    private String stylePreference; // 风格偏好

    private List<String> priorities; // 优先级，比如“收纳、安全、预算”

    private List<String> constraints; // 约束条件，比如“不要复杂吊顶、适合宠物”
}
