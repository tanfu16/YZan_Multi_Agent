package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class SkillExecutionRequest {

    private String userRequest;
    private String location;
    private String materialKeyword;
    private String platform;
    private String furnitureKeyword;
}
