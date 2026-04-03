package com.yzan.yzan_multi_agent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AmapMcpMaterialSearchClientTest {

    @Autowired
    private AmapMcpMaterialSearchClient client;

    @Test
    void shouldPrintAmapMcpTools() {
        client.printAvailableTools();
    }

    @Test
    void shouldPrintMaterialSearchResult() {
        client.searchNearestStores("上海", "瓷砖");
    }
}
