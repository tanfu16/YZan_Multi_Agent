package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * 方案选项
 */
@Data
public class PlanOption {

    private String name; // 方案名称

    private String positioning; // 方案定位

    private List<String> recommendations; // 该方案下的核心建议

    private List<String> advantages; // 该方案优点

    private List<String> disadvantages; // 该方案缺点

    private String applicableCrowd; // 适用人群或场景
}
