package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

/**
 * 冲突
 */
@Data
public class ConflictItem {

    private String topic; // 冲突主题

    private List<String> relatedAgents; // 涉及哪些agent

    private String description; // 冲突描述

    private String resolution; // 最终解决建议
}
