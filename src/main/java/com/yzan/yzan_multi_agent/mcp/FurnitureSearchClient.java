package com.yzan.yzan_multi_agent.mcp;

import com.yzan.yzan_multi_agent.domain.FurnitureSearchResult;

import java.util.List;

public interface FurnitureSearchClient {

    List<FurnitureSearchResult> searchFurniture(String platform, String keyword);
}
