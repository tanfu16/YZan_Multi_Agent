package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class VectorKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final QwenEmbeddingModel qwenEmbeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final PersistedKnowledgeChunkService persistedKnowledgeChunkService;

    public VectorKnowledgeRetrievalService(QwenEmbeddingModel qwenEmbeddingModel,
                                           EmbeddingStore<TextSegment> embeddingStore,
                                           PersistedKnowledgeChunkService persistedKnowledgeChunkService) {
        this.qwenEmbeddingModel = qwenEmbeddingModel;
        this.embeddingStore = embeddingStore;
        this.persistedKnowledgeChunkService = persistedKnowledgeChunkService;
    }

    @Override
    public List<KnowledgeChunk> retrieveForSafety(StructuredRequirement requirement) {
        return retrieve("SAFETY", requirement, "这是一个家庭装修安全知识检索请求。请重点关注儿童安全、宠物友好、防滑设计、圆角处理、耐磨材料和安全动线。");
    }

    @Override
    public List<KnowledgeChunk> retrieveForBudget(StructuredRequirement requirement) {
        return retrieve("BUDGET", requirement, "这是一个家庭装修预算知识检索请求。请重点关注预算分配、成本控制、材料性价比、超支风险和阶段性投入建议。");
    }

    @Override
    public List<KnowledgeChunk> retrieveForLayout(StructuredRequirement requirement) {
        return retrieve("LAYOUT", requirement, "这是一个家庭装修布局知识检索请求。请重点关注空间通透感、动线规划、功能分区、家庭成员适配和空间利用率。");
    }

    @Override
    public List<KnowledgeChunk> retrieveForStorage(StructuredRequirement requirement) {
        return retrieve("STORAGE", requirement, "这是一个家庭装修收纳知识检索请求。请重点关注柜体设计、储物能力、封闭与开放收纳平衡、长期整理便利性和空间压迫感控制。");
    }

    private List<KnowledgeChunk> retrieve(String agentName, StructuredRequirement requirement, String scenarioPrompt) {
        String queryText = buildQuery(requirement, scenarioPrompt);
        System.out.println("========== " + agentName + " Vector Search Query ==========");
        System.out.println(queryText);

        Embedding queryEmbedding = qwenEmbeddingModel.embed(queryText).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        System.out.println(agentName + " vector search matches count = " + matches.size());

        List<KnowledgeChunk> chunks = matches.stream()
                .map(match -> toKnowledgeChunk(match.embedded()))
                .toList();

        printMatchedChunks(agentName, chunks);
        return chunks;
    }

    private void printMatchedChunks(String agentName, List<KnowledgeChunk> chunks) {
        System.out.println("========== " + agentName + " Matched Chunks ==========");

        if (chunks == null || chunks.isEmpty()) {
            System.out.println("No matched chunks.");
            System.out.println("============================================");
            return;
        }

        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            System.out.println("Chunk " + (i + 1));
            System.out.println("Source = " + chunk.getSourceName());
            System.out.println("Content = ");
            System.out.println(chunk.getContent());
            System.out.println("--------------------------------------------");
        }

        System.out.println("============================================");
    }

    private String buildQuery(StructuredRequirement requirement, String scenarioPrompt) {
        return """
                %s
                家庭结构: %s
                风格偏好: %s
                优先级: %s
                约束条件: %s
                """.formatted(
                scenarioPrompt,
                safe(requirement.getFamilyProfile()),
                safe(requirement.getStylePreference()),
                safe(requirement.getPriorities()),
                safe(requirement.getConstraints())
        );
    }

    private KnowledgeChunk toKnowledgeChunk(TextSegment segment) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setContent(segment.text());
        chunk.setSourceName(resolveSourceName(segment.text()));
        return chunk;
    }

    private String resolveSourceName(String content) {
        return persistedKnowledgeChunkService.resolveSourceName(content);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
