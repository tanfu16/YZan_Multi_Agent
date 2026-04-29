package com.yzan.yzan_multi_agent.service;

import com.yzan.yzan_multi_agent.domain.ScalarFieldPatch;
import com.yzan.yzan_multi_agent.domain.StringListPatch;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.StructuredRequirementPatch;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredRequirementStateMergerTest {

    private final StructuredRequirementStateMerger merger = new StructuredRequirementStateMerger();

    @Test
    void shouldKeepUntouchedFieldsAndApplyPatchOperations() {
        StructuredRequirement currentState = new StructuredRequirement();
        currentState.setHouseType("三室两厅");
        currentState.setArea(118);
        currentState.setBudget(new BigDecimal("180000"));
        currentState.setFamilyProfile("夫妻+孩子");
        currentState.setStylePreference("现代简约");
        currentState.setPriorities(List.of("安全", "收纳"));
        currentState.setConstraints(List.of("防滑", "耐磨"));

        StructuredRequirementPatch patch = new StructuredRequirementPatch();
        patch.setBudget(scalar("SET", new BigDecimal("200000"), 0.95));
        patch.setStylePreference(scalar("KEEP", null, 0.95));
        patch.setConstraints(list("REMOVE", List.of("防滑"), 0.95));
        patch.setPriorities(list("ADD", List.of("好打理"), 0.95));

        StructuredRequirement merged = merger.merge(currentState, patch, new UserRequirement());

        assertThat(merged.getHouseType()).isEqualTo("三室两厅");
        assertThat(merged.getBudget()).isEqualByComparingTo("200000");
        assertThat(merged.getStylePreference()).isEqualTo("现代简约");
        assertThat(merged.getConstraints()).containsExactly("耐磨");
        assertThat(merged.getPriorities()).containsExactly("安全", "收纳", "好打理");
    }

    @Test
    void shouldIgnoreLowConfidencePatchAndBackfillFromUserRequirement() {
        StructuredRequirementPatch patch = new StructuredRequirementPatch();
        patch.setStylePreference(scalar("SET", "中古风", 0.4));

        UserRequirement userRequirement = new UserRequirement();
        userRequirement.setHouseType("两室一厅");
        userRequirement.setArea(89);
        userRequirement.setBudget(new BigDecimal("120000"));
        userRequirement.setStylePreference("原木风");
        userRequirement.setFamilyMembers(List.of("夫妻", "孩子"));
        userRequirement.setSpecialNeeds(List.of("收纳", "安全"));

        StructuredRequirement merged = merger.merge(null, patch, userRequirement);

        assertThat(merged.getHouseType()).isEqualTo("两室一厅");
        assertThat(merged.getArea()).isEqualTo(89);
        assertThat(merged.getBudget()).isEqualByComparingTo("120000");
        assertThat(merged.getStylePreference()).isEqualTo("原木风");
        assertThat(merged.getFamilyProfile()).isEqualTo("夫妻+孩子");
        assertThat(merged.getPriorities()).containsExactly("收纳", "安全");
    }

    private <T> ScalarFieldPatch<T> scalar(String operation, T value, double confidence) {
        ScalarFieldPatch<T> patch = new ScalarFieldPatch<>();
        patch.setOperation(operation);
        patch.setValue(value);
        patch.setConfidence(confidence);
        return patch;
    }

    private StringListPatch list(String operation, List<String> values, double confidence) {
        StringListPatch patch = new StringListPatch();
        patch.setOperation(operation);
        patch.setValues(values);
        patch.setConfidence(confidence);
        return patch;
    }
}
