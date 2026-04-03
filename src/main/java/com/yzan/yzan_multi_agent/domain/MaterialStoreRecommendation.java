package com.yzan.yzan_multi_agent.domain;

import lombok.Data;

@Data
public class MaterialStoreRecommendation {

    private String materialKeyword; // 材料关键词
    private String storeName;       // 店铺名称
    private String address;         // 地址
    private String distance;        // 距离
}
