package com.yzan.yzan_multi_agent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlaywrightMcpFurnitureSearchClientTest {

    @Autowired
    private PlaywrightMcpFurnitureSearchClient client;

    @Test
    void shouldPrintPlaywrightMcpTools() {
        client.printAvailableTools();
    }

    @Test
    void shouldSearchJdFurniture() {
        client.searchFurniture("jd", "现代简约沙发");
    }
}
