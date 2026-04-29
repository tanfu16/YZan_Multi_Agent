package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathKnowledgeLoaderTest {

    @Test
    void shouldExpandKnowledgeFilesIntoAgentDomains() {
        ClasspathKnowledgeLoader loader = new ClasspathKnowledgeLoader();

        List<KnowledgeChunk> chunks = loader.loadAllChunks();

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/child-safe-design.md".equals(chunk.getSourceName())
                        && "SAFETY".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/pet-friendly-materials.md".equals(chunk.getSourceName())
                        && "BUDGET".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/pet-friendly-materials.md".equals(chunk.getSourceName())
                        && "LAYOUT".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/corner-and-cabinet-safety.md".equals(chunk.getSourceName())
                        && "STORAGE".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/layout-kitchen-bath-workflow.md".equals(chunk.getSourceName())
                        && "LAYOUT".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/budget-contractor-cost-control.md".equals(chunk.getSourceName())
                        && "BUDGET".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/safety-indoor-air-quality-remodeling.md".equals(chunk.getSourceName())
                        && "SAFETY".equals(chunk.getCategory())));
        assertTrue(chunks.stream().anyMatch(chunk ->
                "RAG/storage-kitchen-laundry-utility-systems.md".equals(chunk.getSourceName())
                        && "STORAGE".equals(chunk.getCategory())));
    }
}
