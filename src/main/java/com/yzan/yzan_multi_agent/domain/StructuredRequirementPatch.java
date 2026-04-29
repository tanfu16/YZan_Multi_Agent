package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StructuredRequirementPatch {

    private ScalarFieldPatch<String> houseType;

    private ScalarFieldPatch<Integer> area;

    private ScalarFieldPatch<BigDecimal> budget;

    private ScalarFieldPatch<String> familyProfile;

    private ScalarFieldPatch<String> stylePreference;

    private StringListPatch priorities;

    private StringListPatch constraints;
}
