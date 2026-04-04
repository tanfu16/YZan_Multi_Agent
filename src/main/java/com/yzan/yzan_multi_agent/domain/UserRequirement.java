package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户请求
 */
@Data
public class UserRequirement {

    private String sessionId;

    private String houseType;

    private Integer area;

    private BigDecimal budget;

    private List<String> familyMembers;

    private String stylePreference;

    private List<String> specialNeeds;

    private String rawDescription;
}
