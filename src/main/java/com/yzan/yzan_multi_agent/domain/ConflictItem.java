package com.yzan.yzan_multi_agent.domain;

import com.yzan.yzan_multi_agent.domain.enums.ConflictSeverity;
import com.yzan.yzan_multi_agent.domain.enums.ConflictType;
import lombok.Data;

import java.util.List;

/**
 * 冲突项
 */
@Data
public class ConflictItem {

    private String topic; // 冲突主题

    private ConflictType conflictType; // 冲突类型

    private ConflictSeverity severity; // 冲突严重程度

    private List<String> relatedAgents; // 涉及哪些 agent

    private String description; // 冲突描述

    private String tradeOff; // 取舍点

    private String chosenDirection; // 最终偏向哪一侧

    private String resolution; // 最终解决建议
}
