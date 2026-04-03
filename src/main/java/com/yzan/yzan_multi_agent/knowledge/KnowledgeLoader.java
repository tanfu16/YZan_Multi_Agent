package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeLoader {

    List<KnowledgeChunk> loadAllChunks();
}
