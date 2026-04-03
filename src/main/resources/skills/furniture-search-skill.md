# furniture-search-skill

## Skill Purpose

This skill is used to search for furniture products on e-commerce pages and return a small set of candidate products.

It is designed for scenarios such as:
- the user asks to find sofas, dining tables, beds, wardrobes, or lamps online
- the user wants several product candidates instead of only design suggestions
- the system wants to enrich a decoration recommendation with actual product search results

This skill is backed by the Playwright MCP service in the current project.

## Trigger Conditions

Trigger this skill when the user intent clearly belongs to one of the following:
- asking to search furniture on an e-commerce platform
- asking for several candidate products online
- asking for platform-based shopping suggestions for furniture

Do not trigger this skill when:
- the user only wants offline store addresses
- the user only wants conceptual design ideas
- the user has not provided enough product keywords to perform a reasonable search

## Required Inputs

Before calling the MCP capability, make sure the following inputs are available:
- `platform`
  - current first version supports `jd`, `jingdong`, or `京东`
- `keyword`
  - such as `现代简约沙发`, `原木餐桌`, `儿童书柜`, `极简落地灯`

## Backing MCP

This skill uses:
- Playwright MCP server

Current Java-side integration:
- `com.yzan.yzan_multi_agent.config.PlaywrightMcpConfig`
- `com.yzan.yzan_multi_agent.mcp.PlaywrightMcpFurnitureSearchClient`

## Expected Output

Return up to 3 product candidates in a structured format.

Each product should ideally include:
- platform
- keyword
- title
- price
- shop name
- link

Suggested presentation:

```text
已为你找到 3 个“现代简约沙发”候选商品：
1. 标题 - 价格 - 店铺 - 链接
2. 标题 - 价格 - 店铺 - 链接
3. 标题 - 价格 - 店铺 - 链接
```

## Failure Handling

If the browser automation flow is blocked by login, risk control, or page structure changes:
- explicitly report that the current platform blocked automated access
- do not fabricate product results
- suggest retrying with another platform or another keyword

## Progressive Disclosure Guidance

This skill should only be disclosed when the user explicitly asks for online furniture search or product candidates.

Do not expose Playwright browsing instructions in the main prompt for general decoration planning tasks.

## Current Project Mapping

In this project, this skill corresponds to:
- MCP type: Playwright MCP
- business client: `FurnitureSearchClient`
- implementation: `PlaywrightMcpFurnitureSearchClient`
