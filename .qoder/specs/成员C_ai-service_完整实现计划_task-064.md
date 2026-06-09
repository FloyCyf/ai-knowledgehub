
# 成员C ai-service 完整实现计划

## Context

成员C负责 AI、MQ、测试、演示视频与文档整合，当前 ai-service 目录仅有 README.md（24行），零 Java 代码，完成度约 7%。本计划将实现 ai-service 的全部代码功能，完成集成配置，并更新 Postman 测试集。演示视频和报告章节属于非代码任务，列入待办清单。

**关键技术决策**：ai-service 使用 Spring MVC（非 WebFlux），SSE 使用 `SseEmitter`，因为 common 模块的 `GlobalExceptionHandler` 依赖 `jakarta.servlet.http.HttpServletRequest`。

---

## Task 1: 创建 ai-service 基础工程结构

### 1.1 创建 `ai-service/pom.xml`
- Parent: `com.ai.knowledgehub:ai-knowledgehub:1.0.0`
- 依赖: common, spring-boot-starter-web, validation, mybatis-plus-spring-boot3-starter, h2, mysql-connector-j, spring-boot-starter-amqp, lombok, spring-boot-starter-test
- **不引入** spring-boot-starter-data-redis（ai-service 不使用 Redis）

### 1.2 创建 `ai-service/src/main/java/com/ai/knowledgehub/ai/AiApplication.java`
- `@SpringBootApplication` + `@MapperScan("com.ai.knowledgehub.ai.mapper")`
- 端口 8084

### 1.3 创建 `ai-service/src/main/resources/application.yml`
- H2 内存数据库 `jdbc:h2:mem:ai_knowledgehub_ai`
- RabbitMQ 连接 localhost:5672
- 手动 ACK 模式
- 自定义配置 `ai.mock.enabled=true`, `ai.mock.model-name=MockLLM-v1`, `ai.mock.streaming-delay-ms=100`, `ai.mock.timeout-ms=30000`

### 1.4 创建 `ai-service/src/main/resources/application-prod.yml`
- MySQL 连接配置
- RabbitMQ 生产环境配置
- `ai.mock.enabled=false`

---

## Task 2: 创建配置类

### 2.1 `ai-service/.../config/DatabaseInitializer.java`
- 实现 `ApplicationRunner`，自动建表 `article_ai_tag` 和 `article_audit_result`
- H2 兼容语法（TEXT 替代 JSON，VARCHAR 替代 ENUM）
- 不设外键（独立 H2 库中 article 表不存在）
- 参照 article-service 的 DatabaseInitializer 模式

### 2.2 `ai-service/.../config/MqConfig.java`
- 声明 FanoutExchange `article.publish.exchange`、Queue `article.tag.queue` / `article.audit.queue` 及 Binding
- 常量值与 article-service 的 MqConfig 完全一致
- **注册 `Jackson2JsonMessageConverter`** Bean（确保 MQ 消息用 JSON 序列化）

### 2.3 `ai-service/.../config/MybatisPlusConfig.java`
- `@MapperScan("com.ai.knowledgehub.ai.mapper")`
- `PaginationInnerInterceptor(DbType.H2)`
- `GlobalConfig` 设置 id-type=AUTO
- 参照 article-service 的 MybatisPlusConfig

### 2.4 修改 `article-service/.../config/OptionalConfig.java`
- 在 `RabbitTemplate` 创建时添加 `template.setMessageConverter(new Jackson2JsonMessageConverter())`
- **这是 MQ 集成的关键修改**：当前 article-service 用默认 SimpleMessageConverter 发送 Map，ai-service 无法用 JSON 反序列化

---

## Task 3: 创建实体和 Mapper

### 3.1 `ai-service/.../entity/ArticleAiTag.java`
- 字段: id(Long, AUTO), articleId(Long), tags(String), modelName(String), createdAt(LocalDateTime)
- **不继承 BaseEntity**（该表无 updateTime/deleted 字段）
- `@TableName("article_ai_tag")`, `@TableId(type=IdType.AUTO)`
- tags 用 String 存储 JSON 字符串（H2 不支持 JSON 列类型）

### 3.2 `ai-service/.../entity/ArticleAuditResult.java`
- 字段: id(Long, AUTO), articleId(Long), result(String), reason(String), modelName(String), createdAt(LocalDateTime)
- **不继承 BaseEntity**

### 3.3 `ai-service/.../mapper/ArticleAiTagMapper.java`
- 继承 `BaseMapper<ArticleAiTag>`

### 3.4 `ai-service/.../mapper/ArticleAuditResultMapper.java`
- 继承 `BaseMapper<ArticleAuditResult>`

---

## Task 4: 创建 DTO 和 VO

### 4.1 `ai-service/.../dto/ContinueWritingDTO.java`
- 字段: prompt(String) + `@NotBlank`

### 4.2 `ai-service/.../vo/ContinueWritingVO.java`
- 字段: content(String), modelName(String)

### 4.3 `ai-service/.../vo/AiTagVO.java`
- 字段: articleId(Long), tags(List\<String\>), modelName(String), createdAt(LocalDateTime)

### 4.4 `ai-service/.../vo/AuditResultVO.java`
- 字段: articleId(Long), result(String), reason(String), modelName(String), createdAt(LocalDateTime)

### 4.5 `ai-service/.../vo/ArticleAnalysisVO.java`
- 字段: tag(AiTagVO), audit(AuditResultVO)

---

## Task 5: 创建 AiService（Mock LLM 核心）

### 5.1 同步续写 `continueWriting(String prompt)`
- 根据关键词匹配返回预设模板文本
- 关键词库：Redis→Redis相关, Spring→Spring相关, AI→AI相关, 默认→通用技术续写

### 5.2 流式续写 `generateStreamingChunks(String prompt)`
- 调用 continueWriting 获取完整文本
- 按标点/固定长度切分为 chunk 列表
- 每个 chunk 对应一个 SSE data 帧

### 5.3 标签提取 `extractTags(Long articleId, String title, String content)`
- 从 title+content 提取关键词
- 返回 3-5 个标签
- Jackson 序列化为 JSON 字符串存入 tags 字段
- 幂等：按 articleId 查询已存在则跳过

### 5.4 合规检测 `auditArticle(Long articleId, String title, String content)`
- 规则：内容空→REVIEW, 含敏感词→REJECT, 其他→PASS
- 附带 reason 说明
- 幂等：按 articleId 查询已存在则跳过

### 5.5 查询分析结果 `getArticleAnalysis(Long articleId)`
- 分别查 article_ai_tag 和 article_audit_result 最新一条
- 组装 ArticleAnalysisVO 返回
- 无数据时返回 null 的对应字段

---

## Task 6: 创建 MQ 消费者

### 6.1 `ai-service/.../mq/ArticleTagConsumer.java`
- `@RabbitListener(queues = MqConfig.ARTICLE_TAG_QUEUE)`
- 接收 `Map<String, Object>` 消息（Jackson2JsonMessageConverter 自动反序列化）
- 提取 articleId/title/content，调用 `aiService.extractTags()`
- 手动 ACK: `channel.basicAck()`
- 失败 NACK 不重入: `channel.basicNack(tag, false, false)`

### 6.2 `ai-service/.../mq/ArticleAuditConsumer.java`
- 结构同 ArticleTagConsumer
- 调用 `aiService.auditArticle()`

---

## Task 7: 创建 AiController

### 7.1 `ai-service/.../controller/AiController.java`
- `@RestController`, `@RequestMapping("/api/ai")`
- **使用 `ApiResponse<T>` 统一返回**（与 coding-standards.md 对齐，区别于 article-service 的 Map 写法）
- **不自行 try-catch**，由 GlobalExceptionHandler 统一处理

| 端点 | 方法 | 返回类型 |
|------|------|----------|
| POST /api/ai/continue-writing | continueWriting | ApiResponse\<ContinueWritingVO\> |
| GET /api/ai/continue-writing/stream | continueWritingStream | SseEmitter |
| GET /api/ai/articles/{id}/analysis | getArticleAnalysis | ApiResponse\<ArticleAnalysisVO\> |

### 7.2 SSE 实现
- `new SseEmitter(timeoutMs)` 设置超时
- `CompletableFuture.runAsync()` 异步发送 chunks
- 注册 onCompletion/onTimeout/onError 回调
- SSE 端点不走 ApiResponse 包装（协议特殊性）

---

## Task 8: 集成配置修改

### 8.1 修改根 `pom.xml`
- 在 `<modules>` 中添加 `<module>ai-service</module>`
- 删除注释"ai-service 由 C 负责创建后再添加"

### 8.2 修改 `gateway-service/src/main/resources/application.yml`
- 在 routes 中添加 ai-service 路由（在 ranking-service 路由之后）
- 路径: `/api/ai/**`，URI: `lb://ai-service`
- 删除注释"ai-service 由 C 负责创建后再添加路由"

---

## Task 9: 更新 Postman 测试集

### 9.1 修改 `postman/AI-KnowledgeHub.postman_collection.json`
- 在 variables 中添加 `aiUrl` 变量（http://localhost:8084）
- 在 item 中添加"AI功能"文件夹，包含 3 个请求：
  1. AI续写(同步) — POST {{aiUrl}}/api/ai/continue-writing
  2. AI流式续写(SSE) — GET {{aiUrl}}/api/ai/continue-writing/stream?prompt=xxx
  3. 获取文章AI分析结果 — GET {{aiUrl}}/api/ai/articles/{{articleId}}/analysis
- 修正 baseUrl 为 http://localhost:8080（网关端口）

---

## Task 10: 验证

- `mvn clean install` 全项目编译通过
- 确认 ai-service 的所有文件结构正确
- 确认 MQ 消息格式与 article-service 发布端一致（Jackson2JsonMessageConverter）
- 确认 gateway 路由配置包含 ai-service

---

## 无法由 AI 完成的待办清单

| 序号 | 待办事项 | 原因 |
|------|----------|------|
| 1 | 演示视频录制（3~5分钟） | 需要实际操作录屏 |
| 2 | 报告章节撰写（5.6、5.7、6、7、8.3、8.5、9.5、9.6） | 需要结合实际运行截图和团队协作体验 |
| 3 | 端到端集成测试（启动 RabbitMQ + 多服务联调） | 需要运行时环境 |
| 4 | Swagger UI 验证（确认 Knife4j 扫描到 ai-service 包） | 需要实际启动验证 |
| 5 | 真实 LLM API 接入（生产环境替换 Mock） | 需要 API Key 和费用 |
| 6 | Nacos 服务发现配置（当前 Gateway 用 lb:// 但无注册中心） | 基础设施问题，需团队协商 |
