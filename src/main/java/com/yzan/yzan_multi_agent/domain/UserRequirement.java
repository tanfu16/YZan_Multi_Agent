package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 用户请求
 */
@Data
public class UserRequirement {

    private String houseType; // 户型，比如“两室一厅”

    private Integer area;

    private BigDecimal budget;

    private List<String> familyMembers;

    private String stylePreference; // 风格偏好

    private List<String> specialNeeds; // 特殊需求，比如“收纳多、好打理”

    private String rawDescription; // 用户原始自由输入
}
