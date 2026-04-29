package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class ScalarFieldPatch<T> {

    private String operation;

    private T value;

    private Double confidence;
}
