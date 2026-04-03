package com.yzan.yzan_multi_agent.mcp;

import com.yzan.yzan_multi_agent.domain.MaterialStoreRecommendation;

import java.util.List;

public interface MaterialSearchClient {

    List<MaterialStoreRecommendation> searchNearestStores(String location, String materialKeyword);
}
