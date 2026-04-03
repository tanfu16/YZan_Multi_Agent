package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class KnowledgeChunk {

    private String sourceName; // 来源文件名
    private String category;   // safety / budget / storage
    private String content;    // 切分后的文本内容
}
