package com.yzan.yzan_multi_agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.MaterialStoreRecommendation;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AmapMcpMaterialSearchClient implements MaterialSearchClient {

    private final McpSyncClient amapMcpClient;
    private final ObjectMapper objectMapper;

    public AmapMcpMaterialSearchClient(McpSyncClient amapMcpClient, ObjectMapper objectMapper) {
        this.amapMcpClient = amapMcpClient;
        this.objectMapper = objectMapper;
    }

    public void printAvailableTools() {
        McpSchema.ListToolsResult tools = amapMcpClient.listTools();
        System.out.println("========== AMap MCP Tools ==========");
        tools.tools().forEach(tool -> {
            System.out.println("Tool name = " + tool.name());
            System.out.println("Description = " + tool.description());
            System.out.println("-----------------------------------");
        });
    }

    @Override
    public List<MaterialStoreRecommendation> searchNearestStores(String location, String materialKeyword) {
        McpSchema.CallToolResult result = amapMcpClient.callTool(
                new McpSchema.CallToolRequest(
                        "maps_text_search",
                        Map.of(
                                "keywords", materialKeyword,
                                "city", location,
                                "offset", 3,
                                "page", 1
                        )
                )
        );

        System.out.println("========== AMap MCP Tool Result ==========");
        System.out.println(result);
        System.out.println("==========================================");

        if (Boolean.TRUE.equals(result.isError())) {
            String errorMessage = extractTextContent(result.content());
            System.out.println("AMap MCP material search failed: " + errorMessage);
            return List.of();
        }

        List<MaterialStoreRecommendation> recommendations = parseStructuredContent(result.structuredContent(), materialKeyword);
        if (!recommendations.isEmpty()) {
            return recommendations.stream().limit(3).toList();
        }

        String textContent = extractTextContent(result.content());
        if (textContent != null && !textContent.isBlank()) {
            recommendations = parseTextAsJson(textContent, materialKeyword);
        }

        return recommendations.stream().limit(3).toList();
    }

    private List<MaterialStoreRecommendation> parseStructuredContent(Object structuredContent, String materialKeyword) {
        if (structuredContent == null) {
            return List.of();
        }

        if (structuredContent instanceof Map<?, ?> structuredMap) {
            return parsePoiContainer(castMap(structuredMap), materialKeyword);
        }

        return List.of();
    }

    private List<MaterialStoreRecommendation> parseTextAsJson(String textContent, String materialKeyword) {
        try {
            Map<String, Object> map = objectMapper.readValue(textContent, new TypeReference<>() {
            });
            return parsePoiContainer(map, materialKeyword);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<MaterialStoreRecommendation> parsePoiContainer(Map<String, Object> container, String materialKeyword) {
        Object pois = container.get("pois");
        if (!(pois instanceof List<?> poiList)) {
            return List.of();
        }

        List<MaterialStoreRecommendation> recommendations = new ArrayList<>();
        for (Object poi : poiList) {
            if (!(poi instanceof Map<?, ?> poiMapRaw)) {
                continue;
            }

            Map<String, Object> poiMap = castMap(poiMapRaw);
            MaterialStoreRecommendation recommendation = new MaterialStoreRecommendation();
            recommendation.setMaterialKeyword(materialKeyword);
            recommendation.setStoreName(firstNonBlank(
                    asString(poiMap.get("name")),
                    asString(poiMap.get("title"))
            ));
            recommendation.setAddress(buildAddress(poiMap));
            recommendation.setDistance(firstNonBlank(
                    asString(poiMap.get("distance")),
                    asString(poiMap.get("shop_distance"))
            ));

            if (recommendation.getStoreName() != null && !recommendation.getStoreName().isBlank()) {
                recommendations.add(recommendation);
            }
        }

        return recommendations;
    }

    private String buildAddress(Map<String, Object> poiMap) {
        String address = asString(poiMap.get("address"));
        if (address != null && !address.isBlank()) {
            return address;
        }

        return joinNonBlank(
                asString(poiMap.get("pname")),
                asString(poiMap.get("cityname")),
                asString(poiMap.get("adname")),
                asString(poiMap.get("name"))
        );
    }

    private String extractTextContent(List<McpSchema.Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (McpSchema.Content content : contents) {
            if (content instanceof McpSchema.TextContent textContent) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(textContent.text());
            }
        }
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> rawMap) {
        return (Map<String, Object>) rawMap;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String joinNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
