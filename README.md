# YZan Multi-Agent

一个基于 Spring Boot + LangChain4j + PostgreSQL/PGVector 的对话式装修助手项目。

当前版本已经实现：

- 统一对话入口：普通聊天、装修方案生成、Skill 调用三类请求共用一条会话链路
- 多 Agent 协作：需求理解 Agent + 4 个并行专业 Agent + 协调 Agent
- RAG 知识检索：按 `LAYOUT / BUDGET / SAFETY / STORAGE` 四个专业域分库检索
- MCP 工具调用：支持线下建材门店查询、线上家具商品搜索
- 会话记忆：短期记忆、长期摘要记忆、独立 `StructuredRequirement` 状态表
- 数据持久化：需求记录、Agent 执行记录、最终方案、知识向量、记忆摘要全部落库

## 1. 项目定位

这个项目想解决的不是“单轮生成一段装修建议”，而是把装修问答拆成更稳定的多步骤流程：

1. 先理解用户当前意图
2. 如果是普通聊天，直接回复
3. 如果是 Skill 请求，调用外部能力
4. 如果是方案生成，提取并维护结构化需求
5. 由 4 个专业 Agent 并行分析
6. 由协调 Agent 汇总成最终方案

主入口接口是：

- `POST /api/conversations/handle`

## 2. 当前已实现的系统结构

### 2.1 会话入口

`RequirementAgent` 是统一入口，负责：

- 判断用户意图：
  - `GENERAL_CHAT`
  - `PLAN_GENERATION`
  - `SKILL_CALL`
- 普通对话时直接生成 `reply`
- 装修需求时输出 `StructuredRequirementPatch`
- Skill 请求时输出 `skillName`

对应结果对象是：

- [RequirementUnderstandingResult.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/domain/RequirementUnderstandingResult.java)

主会话服务：

- [ConversationService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/service/ConversationService.java)
- [ConversationController.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/controller/ConversationController.java)

### 2.2 多 Agent 装修方案工作流

当意图是 `PLAN_GENERATION` 时，系统进入装修方案工作流：

- `LayoutAgent`：布局与动线
- `BudgetAgent`：预算与成本控制
- `SafetyAgent`：儿童 / 老人 / 宠物 / 材料安全
- `StorageAgent`：柜体与收纳规划
- `CoordinatorAgent`：处理冲突、输出主方案和备选方案

核心工作流：

- [DecorationWorkflowService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/workflow/DecorationWorkflowService.java)

### 2.3 并行执行方式

四个专业 Agent 当前通过显式 `ThreadPoolExecutor` 并行执行。

线程池配置：

- [WorkflowExecutorConfig.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/config/WorkflowExecutorConfig.java)

特点：

- 固定 4 个工作线程
- 有界队列 `32`
- 拒绝策略 `CallerRunsPolicy`
- 每个任务通过 `CompletableFuture.supplyAsync(..., agentWorkflowThreadPoolExecutor)` 提交

## 3. 记忆系统

项目当前实现了三层记忆。

### 3.1 短期记忆

使用 LangChain4j 的 `MessageWindowChatMemory`，保留最近 20 条消息。

- [ChatMemoryConfig.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/config/ChatMemoryConfig.java)

特点：

- 按 `sessionId` 隔离
- 存在 JVM 内存中
- 服务重启后失效

### 3.2 长期记忆

长期记忆由两部分组成：

- `conversation_turn_record`：逐轮对话持久化
- `conversation_summary_record`：分段摘要持久化

当会话长度接近短期记忆窗口阈值时，系统会自动生成摘要片段，并在后续对话中注入最近摘要。

核心服务：

- [ConversationMemoryService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/service/ConversationMemoryService.java)

### 3.3 独立状态表

`StructuredRequirement` 不依赖聊天窗口，而是单独维护一份会话真值状态。

当前实现：

- PostgreSQL 持久化
- JVM 热缓存
- 每轮通过 patch 合并更新

核心文件：

- [RequirementStateService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/service/RequirementStateService.java)
- [StructuredRequirementStateMerger.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/service/StructuredRequirementStateMerger.java)
- [RequirementStateRecord.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/persistence/record/RequirementStateRecord.java)

## 4. RAG 设计

### 4.1 为什么要做 RAG

RAG 用来给 4 个专业 Agent 补充稳定、可控、可扩展的装修知识，而不是只依赖模型本身的泛化知识。

### 4.2 当前切分与存储方式

当前知识库流程是：

1. 从 `src/main/resources/RAG` 读取 Markdown 文档
2. 按空行切分成段落 chunk
3. 为每条 chunk 打上专业域标签
4. 通过 DashScope embedding 生成向量
5. 写入 PostgreSQL + PGVector

相关文件：

- [ClasspathKnowledgeLoader.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/knowledge/ClasspathKnowledgeLoader.java)
- [PersistedKnowledgeChunkService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/knowledge/PersistedKnowledgeChunkService.java)
- [VectorKnowledgeRetrievalService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/knowledge/VectorKnowledgeRetrievalService.java)

### 4.3 当前知识域

当前知识库按以下 4 个领域独立检索：

- `LAYOUT`
- `BUDGET`
- `SAFETY`
- `STORAGE`

知识源目录：

- [RAG](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/RAG)

当前已经补充了多份专业文档，例如：

- 厨卫动线与布局
- 通用设计与无障碍
- 装修预算与合同控价
- 节能改造投入
- 室内空气质量与装修安全
- 潮湿、霉变、铅与石棉风险
- 衣柜与柜体规划
- 厨房 / 家政 / 洗衣系统收纳

### 4.4 PGVector 表结构

知识向量存储在：

- `knowledge_chunk_record`

主要字段包括：

- `agent_domain`
- `source_name`
- `content`
- `embedding_json`
- `embedding_vector`

建表脚本：

- [createDatabase.sql](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/sql/createDatabase.sql)

## 5. MCP 与 Skill

### 5.1 当前已接入的能力

项目当前实现了两类可调用外部能力：

- 线下建材门店搜索
- 线上家具商品搜索

相关客户端：

- [AmapMcpMaterialSearchClient.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/mcp/AmapMcpMaterialSearchClient.java)
- [PlaywrightMcpFurnitureSearchClient.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/mcp/PlaywrightMcpFurnitureSearchClient.java)

Skill 执行层：

- [SkillExecutionService.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/skills/SkillExecutionService.java)
- [SkillIntentRouter.java](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/skills/SkillIntentRouter.java)

### 5.2 预算 MCP

仓库中还包含一个独立的预算 MCP 示例文件：

- [RenovationBudgetMcpServer.java](/Users/macmima0000/code/YZan_Multi_Agent/mcp/RenovationBudgetMcpServer.java)

它目前是独立文件，**没有直接接入主流程**，适合后续扩展预算类 Skill 或 Agent 内部工具。

## 6. 前端

项目自带一个简单的对话前端：

- [index.html](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/static/index.html)
- [app.js](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/static/app.js)
- [styles.css](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/static/styles.css)

当前前端支持：

- 普通对话
- 装修方案生成
- Skill 调用
- 显示中间提示与最终结果
- 保持同一个浏览器 `sessionId`

默认启动端口：

- `http://localhost:18080`

## 7. 数据持久化

当前已落库的数据包括：

- 原始需求与结构化需求：`requirement_record`
- Agent 执行记录：`agent_execution_record`
- 最终装修方案：`plan_record`
- 当前需求状态：`requirement_state_record`
- 会话 turn：`conversation_turn_record`
- 会话摘要：`conversation_summary_record`
- RAG chunk 与向量：`knowledge_chunk_record`

完整脚本：

- [createDatabase.sql](/Users/macmima0000/code/YZan_Multi_Agent/src/main/java/com/yzan/yzan_multi_agent/sql/createDatabase.sql)

## 8. 技术栈

- Java 21
- Spring Boot 3.5.6
- LangChain4j 1.12.1
- DashScope / Qwen
- PostgreSQL
- PGVector
- MyBatis
- MCP SDK
- 原生 HTML / CSS / JavaScript

构建文件：

- [pom.xml](/Users/macmima0000/code/YZan_Multi_Agent/pom.xml)

## 9. 快速启动

### 9.1 环境要求

- JDK 21
- Maven Wrapper
- PostgreSQL
- PGVector 扩展
- DashScope API Key
- 如果要跑 Skill：
  - 高德地图相关配置
  - Playwright MCP 可用环境

### 9.2 初始化数据库

先在 PostgreSQL 中创建数据库，并执行：

```sql
\i src/main/java/com/yzan/yzan_multi_agent/sql/createDatabase.sql
```

脚本中会启用：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 9.3 本地配置

项目默认激活 `local` profile：

- [application.yml](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/application.yml)

可以参考示例配置新建本地配置文件：

- [application-local-example.yml](/Users/macmima0000/code/YZan_Multi_Agent/src/main/resources/application-local-example.yml)

建议自行创建：

```text
src/main/resources/application-local.yml
```

并补充至少这些信息：

- DashScope chat model 配置
- DashScope embedding 配置
- PostgreSQL 数据源
- AMap / Playwright MCP 相关配置

### 9.4 启动项目

```bash
./mvnw spring-boot:run
```

启动后访问：

```text
http://localhost:18080
```

健康检查：

```text
GET /api/health
GET /actuator/health
```

## 10. 主要接口

### 会话主入口

```http
POST /api/conversations/handle
Content-Type: application/json
```

示例：

```json
{
  "sessionId": "demo-session-001",
  "rawDescription": "我家118平三室两厅，预算18万，夫妻加一个孩子和一只狗，想做现代简约风，希望更安全、耐脏、好打理，也要有收纳。"
}
```

### 直接生成方案

```http
POST /api/plans/generate
```

### 直接调用 Skill

```http
POST /api/skills/execute
```

## 11. 测试

运行全量测试：

```bash
./mvnw test
```

当前仓库已经覆盖：

- RequirementAgent
- ConversationService
- 4 个专业 Agent
- CoordinatorAgent
- RAG 加载与持久化
- MCP 客户端
- Skill 执行
- DecorationWorkflowService

## 12. 当前项目特点

这个项目目前比较有代表性的点，不在于“模型调了一次接口”，而在于它已经把下面几件事串起来了：

- 会话理解
- 独立需求状态维护
- 长短期记忆配合
- 多 Agent 并行分析
- RAG 分域检索
- MCP / Skill 外部能力调用
- 最终方案持久化

如果你想把它继续往毕业设计、课程项目或作品集方向整理，这一版已经有比较完整的“系统”形态，而不只是一个单点 demo。
