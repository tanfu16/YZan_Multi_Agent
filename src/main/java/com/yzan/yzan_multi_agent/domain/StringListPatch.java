package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.util.List;

@Data
public class StringListPatch {

    private String operation;

    private List<String> values;

    private Double confidence;
}
