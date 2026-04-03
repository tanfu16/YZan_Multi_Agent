package com.yzan.yzan_multi_agent.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PlaywrightMcpConfig {

    @Bean(name = "playwrightMcpClient")
    public McpSyncClient playwrightMcpClient(
            @Value("${playwright.mcp.command:npx.cmd}") String command,
            McpJsonMapper mcpJsonMapper
    ) {
        ServerParameters params = ServerParameters.builder(command)
                .args("-y", "@playwright/mcp@latest")
                .build();

        StdioClientTransport transport = new StdioClientTransport(params, mcpJsonMapper);
        transport.setStdErrorHandler(line -> System.out.println("[playwright-mcp] " + line));

        McpClientTransport clientTransport = transport;

        McpSyncClient client = McpClient.sync(clientTransport)
                .requestTimeout(Duration.ofSeconds(60))
                .build();

        client.initialize();
        return client;
    }
}
