package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ClasspathKnowledgeLoader implements KnowledgeLoader {

    private static final Map<String, List<String>> KNOWLEDGE_FILE_DOMAINS = Map.ofEntries(
            Map.entry("RAG/anti-slip-and-flooring.md", List.of("SAFETY", "BUDGET")),
            Map.entry("RAG/budget-contractor-cost-control.md", List.of("BUDGET")),
            Map.entry("RAG/budget-energy-efficiency-upgrades.md", List.of("BUDGET")),
            Map.entry("RAG/child-safe-design.md", List.of("SAFETY")),
            Map.entry("RAG/corner-and-cabinet-safety.md", List.of("SAFETY", "STORAGE")),
            Map.entry("RAG/elderly-safety.md", List.of("SAFETY", "LAYOUT")),
            Map.entry("RAG/layout-kitchen-bath-workflow.md", List.of("LAYOUT")),
            Map.entry("RAG/layout-universal-design-accessibility.md", List.of("LAYOUT")),
            Map.entry("RAG/pet-friendly-materials.md", List.of("SAFETY", "BUDGET", "LAYOUT", "STORAGE")),
            Map.entry("RAG/safety-indoor-air-quality-remodeling.md", List.of("SAFETY")),
            Map.entry("RAG/safety-moisture-lead-asbestos-remodeling.md", List.of("SAFETY")),
            Map.entry("RAG/storage-closet-cabinet-planning.md", List.of("STORAGE")),
            Map.entry("RAG/storage-kitchen-laundry-utility-systems.md", List.of("STORAGE"))
    );

    @Override
    public List<KnowledgeChunk> loadAllChunks() {
        List<KnowledgeChunk> chunks = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : KNOWLEDGE_FILE_DOMAINS.entrySet()) {
            String filePath = entry.getKey();
            String content = readFileContent(filePath);
            if (content == null || content.isBlank()) {
                continue;
            }

            List<String> paragraphs = splitIntoParagraphs(content);
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    continue;
                }

                for (String domain : entry.getValue()) {
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setSourceName(filePath);
                    chunk.setCategory(domain);
                    chunk.setContent(paragraph.trim());
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    private String readFileContent(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {
                System.out.println("Knowledge file not found: " + filePath);
                return null;
            }

            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
            }

            return builder.toString();
        } catch (Exception e) {
            System.out.println("Failed to read knowledge file: " + filePath + ", error: " + e.getMessage());
            return null;
        }
    }

    private List<String> splitIntoParagraphs(String content) {
        String normalized = content.replace("\r\n", "\n");
        String[] rawParts = normalized.split("\n\\s*\n");

        List<String> paragraphs = new ArrayList<>();
        for (String rawPart : rawParts) {
            String paragraph = rawPart.trim();
            if (paragraph.length() >= 30) {
                paragraphs.add(paragraph);
            }
        }

        return paragraphs;
    }
}
