# material-store-search-skill

## Skill Purpose

This skill is used to search for the nearest three offline stores where users can buy decoration materials.

It is designed for scenarios such as:
- the user asks where to buy a material nearby
- the user asks for nearby stores for tiles, paint, flooring, hardware, cabinets, lights, or other decoration materials
- the system wants to enrich a decoration plan with local purchasing suggestions

This skill is backed by the AMap MCP service in the current project.

## Trigger Conditions

Trigger this skill when the user intent clearly belongs to one of the following:
- asking for nearby purchase locations for decoration materials
- asking where to buy a specific material offline
- asking for the nearest stores related to a material keyword

Do not trigger this skill when:
- the user only wants online shopping links
- the user only wants design advice without any store search
- the user only wants general material recommendations instead of nearby stores

## Required Inputs

Before calling the MCP capability, make sure the following inputs are available:
- `location`
  - city name, district, or user-provided location context
- `materialKeyword`
  - such as `防滑地砖`, `乳胶漆`, `定制柜板材`, `灯具`, `五金`

## Backing MCP

This skill uses:
- AMap Maps MCP server

Current Java-side integration:
- `com.yzan.yzan_multi_agent.config.AmapMcpConfig`
- `com.yzan.yzan_multi_agent.mcp.AmapMcpMaterialSearchClient`

## Expected Output

Return up to 3 store recommendations in a structured format.

Each recommendation should include:
- material keyword
- store name
- address
- distance

Suggested presentation:

```text
已为你找到附近 3 个“防滑地砖”购买地点：
1. 门店名 - 地址 - 距离
2. 门店名 - 地址 - 距离
3. 门店名 - 地址 - 距离
```

## Failure Handling

If the MCP call fails or returns no valid stores:
- clearly say that nearby store search is temporarily unavailable or no matching stores were found
- do not fabricate addresses
- optionally suggest changing the city or using a broader material keyword

## Progressive Disclosure Guidance

This skill should only be disclosed when the user explicitly asks for offline purchasing locations or nearby stores.

Do not inject AMap store-search instructions into the main system prompt for all requests.

## Current Project Mapping

In this project, this skill corresponds to:
- MCP type: AMap MCP
- business client: `MaterialSearchClient`
- implementation: `AmapMcpMaterialSearchClient`
