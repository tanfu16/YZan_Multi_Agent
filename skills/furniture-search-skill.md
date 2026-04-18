# Furniture Search Skill

## 1. Skill 作用

这个 skill 用于在用户想在线上搜索家具商品时，调用 Playwright MCP 打开电商网页并提取候选商品。

它解决的问题是：
- 用户知道想要什么家具
- 想让系统代替人工去电商平台搜索
- 希望返回前 3 个可参考的商品结果

当前对应的外部能力：
- Playwright MCP

当前项目中的实现承载：
- `PlaywrightMcpFurnitureSearchClient`

---

## 2. 触发条件

当用户明确表达以下意图之一时，应该触发这个 skill：

- 想让系统搜索某类家具商品
- 想比较线上家具候选
- 想让系统去京东/电商平台找家具
- 想获取商品标题、价格、店铺、链接

典型用户表达：
- “帮我找几款现代简约沙发”
- “帮我搜一下京东上的餐桌”
- “我想看看网上有哪些原木风书柜”

不应该触发的情况：
- 用户只是在问装修方案，不需要商品搜索
- 用户只想知道线下门店地址
- 用户没有明确的家具关键词

---

## 3. 所需输入

至少需要以下输入：

- `platform`
  - 当前第一版优先支持：`jd`
  - 后续可扩展：淘宝、天猫、拼多多等

- `keyword`
  - 家具关键词
  - 例如：`现代简约沙发`、`原木风餐桌`、`收纳书柜`

---

## 4. 调用的 MCP 能力

本 skill 对应：
- Playwright MCP

当前主要用到的浏览器工具包括：
- `browser_navigate`
- `browser_wait_for`
- `browser_run_code`

后续可能继续用到：
- `browser_click`
- `browser_type`
- `browser_snapshot`
- `browser_evaluate`

当前项目中由以下客户端承接：
- `FurnitureSearchClient`
- `PlaywrightMcpFurnitureSearchClient`

---

## 5. 输出格式要求

输出必须尽量结构化，至少包含以下字段：

- `platform`
- `keyword`
- `title`
- `price`
- `shopName`
- `link`

建议一次返回前 3 条候选商品。

返回示例：

```json
[
  {
    "platform": "jd",
    "keyword": "现代简约沙发",
    "title": "现代简约小户型布艺沙发",
    "price": "2599",
    "shopName": "某某家具旗舰店",
    "link": "https://item.jd.com/xxxx.html"
  }
]
```

---

## 6. 降级策略

如果 Playwright MCP 调用失败：
- 不要假装已经抓到商品
- 明确返回“当前平台搜索失败”

如果目标平台触发风控、登录页或验证页：
- 明确说明“当前平台限制访问或需要登录态”
- 可以建议切换平台
- 或提示后续使用持久化浏览器会话

如果页面结构变化导致抓取为空：
- 明确说明“页面结构变化，当前选择器未命中”
- 不要返回伪造商品

---

## 7. 使用说明

这个 skill 不是装修建议 skill，而是采购候选搜索 skill。

它适合在以下场景中使用：
- 用户已经明确家具方向
- 需要补充线上商品候选
- 想把“方案建议”延伸到“可执行采购参考”

不建议默认每次都调用，因为：
- 电商页面有风控
- 自动化搜索成本高
- 只应在用户确实需要商品搜索时启用

---

## 8. 当前项目中的对应关系

当前 skill -> MCP -> Java 承接关系如下：

- `furniture-search-skill`
- Playwright MCP
- `PlaywrightMcpConfig`
- `PlaywrightMcpFurnitureSearchClient`

这份 skill 负责说明：
- 什么时候应该调用 Playwright MCP
- 输入要长什么样
- 输出要落成什么结构
- 风控和失败时如何降级
