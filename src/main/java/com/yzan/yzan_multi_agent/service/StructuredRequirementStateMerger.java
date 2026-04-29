package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.ScalarFieldPatch;
import com.yzan.yzan_multi_agent.domain.StringListPatch;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.StructuredRequirementPatch;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class StructuredRequirementStateMerger {

    private static final double DEFAULT_CONFIDENCE = 1.0;
    private static final double MIN_CONFIDENCE = 0.75;

    public StructuredRequirement merge(StructuredRequirement currentState,
                                       StructuredRequirementPatch patch,
                                       UserRequirement userRequirement) {
        StructuredRequirement merged = copy(currentState);
        if (patch != null) {
            merged.setHouseType(applyScalarPatch(merged.getHouseType(), patch.getHouseType()));
            merged.setArea(applyScalarPatch(merged.getArea(), patch.getArea()));
            merged.setBudget(applyScalarPatch(merged.getBudget(), patch.getBudget()));
            merged.setFamilyProfile(applyScalarPatch(merged.getFamilyProfile(), patch.getFamilyProfile()));
            merged.setStylePreference(applyScalarPatch(merged.getStylePreference(), patch.getStylePreference()));
            merged.setPriorities(applyListPatch(merged.getPriorities(), patch.getPriorities()));
            merged.setConstraints(applyListPatch(merged.getConstraints(), patch.getConstraints()));
        }

        backfillFromUserRequirement(merged, userRequirement);
        normalizeLists(merged);
        return merged;
    }

    private <T> T applyScalarPatch(T currentValue, ScalarFieldPatch<T> patch) {
        if (patch == null) {
            return currentValue;
        }
        String operation = normalizeOperation(patch.getOperation(), "KEEP");
        if (!isConfident(patch.getConfidence())) {
            return currentValue;
        }
        return switch (operation) {
            case "SET", "REPLACE" -> patch.getValue() != null ? patch.getValue() : currentValue;
            case "REMOVE", "CLEAR", "DELETE" -> null;
            default -> currentValue;
        };
    }

    private List<String> applyListPatch(List<String> currentValues, StringListPatch patch) {
        List<String> base = deduplicate(currentValues);
        if (patch == null) {
            return base;
        }
        String operation = normalizeOperation(patch.getOperation(), "KEEP");
        if (!isConfident(patch.getConfidence())) {
            return base;
        }
        List<String> values = deduplicate(patch.getValues());
        return switch (operation) {
            case "REPLACE", "SET" -> values;
            case "REMOVE" -> removeAll(base, values);
            case "CLEAR", "DELETE" -> new ArrayList<>();
            case "ADD" -> addAll(base, values);
            default -> base;
        };
    }

    private void backfillFromUserRequirement(StructuredRequirement merged, UserRequirement userRequirement) {
        if (userRequirement == null) {
            return;
        }
        if (!hasText(merged.getHouseType()) && hasText(userRequirement.getHouseType())) {
            merged.setHouseType(userRequirement.getHouseType().trim());
        }
        if (merged.getArea() == null && userRequirement.getArea() != null) {
            merged.setArea(userRequirement.getArea());
        }
        if (merged.getBudget() == null && userRequirement.getBudget() != null) {
            merged.setBudget(userRequirement.getBudget());
        }
        if (!hasText(merged.getFamilyProfile()) && userRequirement.getFamilyMembers() != null && !userRequirement.getFamilyMembers().isEmpty()) {
            merged.setFamilyProfile(String.join("+", userRequirement.getFamilyMembers()));
        }
        if (!hasText(merged.getStylePreference()) && hasText(userRequirement.getStylePreference())) {
            merged.setStylePreference(userRequirement.getStylePreference().trim());
        }
        if ((merged.getPriorities() == null || merged.getPriorities().isEmpty())
                && userRequirement.getSpecialNeeds() != null && !userRequirement.getSpecialNeeds().isEmpty()) {
            merged.setPriorities(deduplicate(userRequirement.getSpecialNeeds()));
        }
    }

    private void normalizeLists(StructuredRequirement merged) {
        merged.setPriorities(deduplicate(merged.getPriorities()));
        merged.setConstraints(deduplicate(merged.getConstraints()));
    }

    private StructuredRequirement copy(StructuredRequirement source) {
        StructuredRequirement target = new StructuredRequirement();
        if (source == null) {
            target.setPriorities(new ArrayList<>());
            target.setConstraints(new ArrayList<>());
            return target;
        }
        target.setHouseType(source.getHouseType());
        target.setArea(source.getArea());
        target.setBudget(source.getBudget() == null ? null : new BigDecimal(source.getBudget().toPlainString()));
        target.setFamilyProfile(source.getFamilyProfile());
        target.setStylePreference(source.getStylePreference());
        target.setPriorities(deduplicate(source.getPriorities()));
        target.setConstraints(deduplicate(source.getConstraints()));
        return target;
    }

    private List<String> deduplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (hasText(value)) {
                unique.add(value.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    private List<String> addAll(List<String> base, List<String> additions) {
        Set<String> merged = new LinkedHashSet<>(deduplicate(base));
        merged.addAll(deduplicate(additions));
        return new ArrayList<>(merged);
    }

    private List<String> removeAll(List<String> base, List<String> removals) {
        Set<String> remaining = new LinkedHashSet<>(deduplicate(base));
        for (String value : deduplicate(removals)) {
            remaining.remove(value);
        }
        return new ArrayList<>(remaining);
    }

    private boolean isConfident(Double confidence) {
        return confidence == null || confidence >= MIN_CONFIDENCE;
    }

    private String normalizeOperation(String operation, String defaultValue) {
        if (operation == null || operation.isBlank()) {
            return defaultValue;
        }
        return operation.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
