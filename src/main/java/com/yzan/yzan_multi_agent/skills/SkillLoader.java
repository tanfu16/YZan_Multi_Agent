package com.yzan.yzan_multi_agent.skills;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SkillLoader {

    private static final String SKILL_RESOURCE_PATTERN = "classpath*:skills/*.md";

    public List<SkillDefinition> loadAllSkills() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SKILL_RESOURCE_PATTERN);

            List<SkillDefinition> skills = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || "README.md".equalsIgnoreCase(filename)) {
                    continue;
                }

                SkillDefinition skill = new SkillDefinition();
                skill.setSkillName(removeMdSuffix(filename));
                skill.setResourcePath("skills/" + filename);
                skill.setContent(readResource(resource));
                skills.add(skill);
            }

            skills.sort(Comparator.comparing(SkillDefinition::getSkillName));
            return skills;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load skill documents from resources.", e);
        }
    }

    public Optional<SkillDefinition> loadSkill(String skillName) {
        return loadAllSkills().stream()
                .filter(skill -> skill.getSkillName().equals(skillName))
                .findFirst();
    }

    private String readResource(Resource resource) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString().trim();
    }

    private String removeMdSuffix(String filename) {
        return filename.endsWith(".md")
                ? filename.substring(0, filename.length() - 3)
                : filename;
    }
}
