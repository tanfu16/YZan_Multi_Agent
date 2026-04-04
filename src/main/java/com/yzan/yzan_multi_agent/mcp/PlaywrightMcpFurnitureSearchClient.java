package com.yzan.yzan_multi_agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzan.yzan_multi_agent.domain.FurnitureSearchResult;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PlaywrightMcpFurnitureSearchClient implements FurnitureSearchClient {

    private final McpSyncClient playwrightMcpClient;
    private final ObjectMapper objectMapper;

    public PlaywrightMcpFurnitureSearchClient(
            @Qualifier("playwrightMcpClient") McpSyncClient playwrightMcpClient,
            ObjectMapper objectMapper
    ) {
        this.playwrightMcpClient = playwrightMcpClient;
        this.objectMapper = objectMapper;
    }

    public void printAvailableTools() {
        McpSchema.ListToolsResult tools = playwrightMcpClient.listTools();
        System.out.println("========== Playwright MCP Tools ==========");
        tools.tools().forEach(tool -> {
            System.out.println("Tool name = " + tool.name());
            System.out.println("Description = " + tool.description());
            System.out.println("-----------------------------------------");
        });
    }

    @Override
    public List<FurnitureSearchResult> searchFurniture(String platform, String keyword) {
        if (!isJdPlatform(platform)) {
            System.out.println("Playwright furniture search currently supports JD only. Input platform = " + platform);
            return List.of();
        }

        String normalizedKeyword = normalizeKeyword(keyword);
        String url = buildJdSearchUrl(normalizedKeyword);
        System.out.println("========== Playwright JD Search ==========");
        System.out.println("Keyword = " + normalizedKeyword);
        System.out.println("URL = " + url);

        McpSchema.CallToolResult navigateResult = playwrightMcpClient.callTool(
                new McpSchema.CallToolRequest(
                        "browser_navigate",
                        Map.of("url", url)
                )
        );
        logToolResult("browser_navigate", navigateResult);
        if (Boolean.TRUE.equals(navigateResult.isError())) {
            return List.of();
        }

        McpSchema.CallToolResult waitResult = playwrightMcpClient.callTool(
                new McpSchema.CallToolRequest(
                        "browser_wait_for",
                        Map.of("time", 5)
                )
        );
        logToolResult("browser_wait_for", waitResult);
        if (Boolean.TRUE.equals(waitResult.isError())) {
            return List.of();
        }

        String code = """
                async (page) => {
                  await page.waitForLoadState('domcontentloaded');
                  await page.waitForTimeout(3000);

                  return await page.evaluate(() => {
                    const itemSelectors = [
                      '.gl-item',
                      '.j-sku-item',
                      '[data-sku]',
                      '.search_prolist_item',
                      '.goods-list-v2 .goods-item',
                      '.s-item'
                    ];

                    const titleSelectors = [
                      '.p-name em',
                      '.p-name a em',
                      '.p-name-type-2 em',
                      '.sku-name',
                      '.title',
                      'a[title]',
                      '.p-name a'
                    ];

                    const priceSelectors = [
                      '.p-price i',
                      '.p-price strong i',
                      '.price i',
                      '.price',
                      '[class*=price] i',
                      '[class*=price]'
                    ];

                    const shopSelectors = [
                      '.curr-shop',
                      '.p-shop a',
                      '.p-shopnum a',
                      '[class*=shop] a',
                      '.store-name',
                      '.shop-name'
                    ];

                    const normalizeText = (text) => (text || '').replace(/\\s+/g, ' ').trim();

                    const pickText = (root, selectors) => {
                      for (const selector of selectors) {
                        const node = root.querySelector(selector);
                        const text = normalizeText(node?.textContent);
                        if (text) {
                          return text;
                        }
                      }
                      return '';
                    };

                    const normalizeLink = (href) => {
                      if (!href) {
                        return '';
                      }
                      if (href.startsWith('//')) {
                        return 'https:' + href;
                      }
                      if (href.startsWith('/')) {
                        return window.location.origin + href;
                      }
                      return href;
                    };

                    const parsePriceFromText = (text) => {
                      const normalized = normalizeText(text);
                      const match = normalized.match(/\\b(\\d{2,6}(?:\\.\\d{1,2})?)\\b/);
                      return match ? match[1] : '';
                    };

                    const parseShopFromText = (text) => {
                      const normalized = normalizeText(text);
                      const segments = normalized.split(/\\s+/);
                      return segments.find(segment =>
                        segment.includes('店') ||
                        segment.includes('旗舰') ||
                        segment.includes('自营') ||
                        segment.includes('经营部')
                      ) || '';
                    };

                    const linkCandidates = Array.from(
                      document.querySelectorAll('a[href*="item.jd.com"], a[href*="//item.jd.com/"]')
                    ).map(link => {
                      const container = link.closest('[data-sku], .gl-item, .j-sku-item, li, div') || link.parentElement || link;
                      const title = normalizeText(link.getAttribute('title')) || normalizeText(link.textContent) || pickText(container, titleSelectors);
                      const price = pickText(container, priceSelectors) || parsePriceFromText(container.textContent);
                      const shopName = pickText(container, shopSelectors) || parseShopFromText(container.textContent);
                      const href = normalizeLink(link.href || link.getAttribute('href'));

                      return {
                        title,
                        price,
                        shopName,
                        link: href
                      };
                    });

                    const deduplicatedByLink = [];
                    const seenLinks = new Set();
                    for (const item of linkCandidates) {
                      if (!item.title || !item.link) {
                        continue;
                      }
                      if (seenLinks.has(item.link)) {
                        continue;
                      }
                      seenLinks.add(item.link);
                      deduplicatedByLink.push(item);
                    }

                    if (deduplicatedByLink.length > 0) {
                      return deduplicatedByLink.slice(0, 3);
                    }

                    let items = [];
                    for (const selector of itemSelectors) {
                      const found = Array.from(document.querySelectorAll(selector));
                      if (found.length > 0) {
                        items = found;
                        break;
                      }
                    }

                    return items
                      .slice(0, 10)
                      .map(item => ({
                        title: pickText(item, titleSelectors) || normalizeText(item.textContent).slice(0, 80),
                        price: pickText(item, priceSelectors) || parsePriceFromText(item.textContent),
                        shopName: pickText(item, shopSelectors) || parseShopFromText(item.textContent),
                        link: normalizeLink(item.querySelector('a[href]')?.href || item.querySelector('a[href]')?.getAttribute('href'))
                      }))
                      .filter(item => item.title && item.link)
                      .slice(0, 3);
                  });
                }
                """;

        McpSchema.CallToolResult extractResult = playwrightMcpClient.callTool(
                new McpSchema.CallToolRequest(
                        "browser_run_code",
                        Map.of("code", code)
                )
        );
        logToolResult("browser_run_code", extractResult);
        if (Boolean.TRUE.equals(extractResult.isError())) {
            return List.of();
        }

        List<FurnitureSearchResult> results = parseFurnitureResults(extractResult, platform, normalizedKeyword);
        System.out.println("Parsed furniture result count = " + results.size());
        results.forEach(item -> {
            System.out.println("Title = " + item.getTitle());
            System.out.println("Price = " + item.getPrice());
            System.out.println("Shop = " + item.getShopName());
            System.out.println("Link = " + item.getLink());
            System.out.println("-----------------------------------------");
        });
        System.out.println("=========================================");

        return results;
    }

    private boolean isJdPlatform(String platform) {
        if (platform == null) {
            return false;
        }
        String normalized = platform.trim().toLowerCase();
        return "jd".equals(normalized) || "jingdong".equals(normalized) || "京东".equals(platform.trim());
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "sofa";
        }
        return keyword;
    }

    private String buildJdSearchUrl(String keyword) {
        return "https://search.jd.com/Search?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&enc=utf-8";
    }

    private List<FurnitureSearchResult> parseFurnitureResults(McpSchema.CallToolResult result, String platform, String keyword) {
        List<FurnitureSearchResult> parsedFromStructured = parseStructuredContent(result.structuredContent(), platform, keyword);
        if (!parsedFromStructured.isEmpty()) {
            return parsedFromStructured;
        }

        String textContent = extractTextContent(result.content());
        String extractedJson = extractJsonResultBlock(textContent);
        if (extractedJson == null || extractedJson.isBlank()) {
            return List.of();
        }

        try {
            List<Map<String, Object>> items = objectMapper.readValue(extractedJson, new TypeReference<>() {
            });
            return toFurnitureResults(items, platform, keyword);
        } catch (Exception e) {
            System.out.println("Failed to parse Playwright result JSON: " + e.getMessage());
            return List.of();
        }
    }

    private List<FurnitureSearchResult> parseStructuredContent(Object structuredContent, String platform, String keyword) {
        if (structuredContent instanceof List<?> rawList) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object rawItem : rawList) {
                if (rawItem instanceof Map<?, ?> rawMap) {
                    items.add(castMap(rawMap));
                }
            }
            return toFurnitureResults(items, platform, keyword);
        }

        if (structuredContent instanceof Map<?, ?> rawMap) {
            Object items = rawMap.get("result");
            if (items instanceof List<?> itemList) {
                List<Map<String, Object>> maps = new ArrayList<>();
                for (Object item : itemList) {
                    if (item instanceof Map<?, ?> itemMap) {
                        maps.add(castMap(itemMap));
                    }
                }
                return toFurnitureResults(maps, platform, keyword);
            }
        }

        return List.of();
    }

    private List<FurnitureSearchResult> toFurnitureResults(List<Map<String, Object>> items, String platform, String keyword) {
        List<FurnitureSearchResult> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            FurnitureSearchResult result = new FurnitureSearchResult();
            result.setPlatform(platform);
            result.setKeyword(keyword);
            result.setTitle(asString(item.get("title")));
            result.setPrice(asString(item.get("price")));
            result.setShopName(asString(item.get("shopName")));
            result.setLink(asString(item.get("link")));

            if (result.getTitle() != null && !result.getTitle().isBlank()) {
                results.add(result);
            }
        }
        return results.stream().limit(3).toList();
    }

    private String extractJsonResultBlock(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return null;
        }

        String startMarker = "### Result";
        int startIndex = textContent.indexOf(startMarker);
        if (startIndex < 0) {
            return textContent;
        }

        String remaining = textContent.substring(startIndex + startMarker.length()).trim();
        int nextSectionIndex = remaining.indexOf("### ");
        if (nextSectionIndex >= 0) {
            return remaining.substring(0, nextSectionIndex).trim();
        }
        return remaining.trim();
    }

    private void logToolResult(String toolName, McpSchema.CallToolResult result) {
        System.out.println("========== Playwright Tool Result ==========");
        System.out.println("Tool = " + toolName);
        System.out.println(result);
        System.out.println("===========================================");
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
}
