package com.yzan.yzan_multi_agent.knowledge;

import com.yzan.yzan_multi_agent.domain.KnowledgeChunk;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClasspathKnowledgeLoader implements KnowledgeLoader {

    private static final List<String> KNOWLEDGE_FILES = List.of(
            "RAG/anti-slip-and-flooring.md",
            "RAG/child-safe-design.md",
            "RAG/corner-and-cabinet-safety.md",
            "RAG/elderly-safety.md",
            "RAG/pet-friendly-materials.md"
    );


    @Override
    public List<KnowledgeChunk> loadAllChunks() {
        List<KnowledgeChunk> chunks = new ArrayList<>();

        for (String filePath : KNOWLEDGE_FILES) {
            String content = readFileContent(filePath);
            if (content == null || content.isBlank()) {
                continue;
            }

            List<String> paragraphs = splitIntoParagraphs(content);
            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) {
                    continue;
                }

                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setSourceName(filePath);
                chunk.setContent(paragraph.trim());
                chunks.add(chunk);
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
