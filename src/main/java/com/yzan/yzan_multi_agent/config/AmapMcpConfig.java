package com.yzan.yzan_multi_agent.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class AmapMcpConfig {

    @Bean
    public McpJsonMapper mcpJsonMapper() {
        return new JacksonMcpJsonMapperSupplier().get();
    }

    @Bean
    public McpSyncClient amapMcpClient(
            @Value("${amap.mcp.api-key}") String apiKey,
            @Value("${amap.mcp.command:npx.cmd}") String command,
            McpJsonMapper mcpJsonMapper
    ) {
        ServerParameters params = ServerParameters.builder(command)
                .args("-y", "@amap/amap-maps-mcp-server")
                .env(Map.of("AMAP_MAPS_API_KEY", apiKey))
                .build();

        StdioClientTransport transport = new StdioClientTransport(params, mcpJsonMapper);
        transport.setStdErrorHandler(line -> System.out.println("[amap-mcp] " + line));

        McpClientTransport clientTransport = transport;

        McpSyncClient client = McpClient.sync(clientTransport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        client.initialize();
        return client;
    }
}
