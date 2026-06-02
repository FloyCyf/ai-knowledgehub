# 详细任务列表

本文档按开发阶段拆分，可直接作为项目 Todo 使用。

## 阶段 0：项目初始化

### 0.1 创建代码仓库

- 创建 Git 仓库。
- 建立统一项目目录。
- 添加 README。
- 添加 `.gitignore`。
- 确定代码规范。

建议目录：

```text
ai-knowledgehub/
├── gateway-service/
├── user-service/
├── article-service/
├── ranking-service/
├── ai-service/
├── common/
├── docs/
├── postman/
├── docker-compose.yml
└── README.md
```

### 0.2 编写 Docker Compose

需要启动：

- MySQL。
- Redis。
- RabbitMQ。
- Nacos 可选。

最低要求：MySQL + Redis + RabbitMQ。

### 0.3 统一接口规范

包括：

- 返回格式统一。
- 错误码统一。
- JWT Header 规范。
- 分页参数规范。
- 时间格式规范。

统一返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 阶段 1：基础支撑功能开发

### 1.1 用户注册

- 用户名唯一校验。
- 密码加密存储。
- 默认角色为普通用户。
- 支持管理员角色初始化。
- 数据库表：`user`。

### 1.2 用户登录

- 校验用户名密码。
- 登录成功生成 JWT Token。
- 返回用户基本信息和角色。

### 1.3 JWT 鉴权

- 网关解析 Token。
- 校验 Token 是否有效。
- 将用户 ID、角色传给后端服务。
- 对管理员接口做权限拦截。

### 1.4 注销登录

- 简单版：前端丢弃 Token。
- 加分版：Redis 维护 Token 黑名单。

## 阶段 2：文章内容管理

### 2.1 创建文章草稿

- 登录用户创建草稿。
- 文章状态为 `DRAFT`。
- 保存标题、正文、作者 ID。

### 2.2 修改文章

- 作者本人可以修改。
- 管理员可以修改。
- 已删除文章不能修改。

### 2.3 发布文章

- 状态从 `DRAFT` 改为 `PUBLISHED`。
- 设置发布时间。
- 发送 MQ 消息：`ArticlePublishedEvent`。

消息示例：

```json
{
  "articleId": 1001,
  "title": "Redis ZSET 实现热榜",
  "authorId": 12,
  "content": "文章正文...",
  "publishTime": "2026-06-02 20:00:00"
}
```

### 2.4 逻辑删除文章

- 修改 `deleted` 字段。
- 不物理删除数据库记录。

### 2.5 分页获取最新文章

- 只返回已发布、未删除文章。
- 按发布时间倒序。
- 支持分页。

### 2.6 获取文章详情

- 查询文章详情。
- 阅读量增加。
- Redis 热度 +1。
- 可选：文章详情缓存。

## 阶段 3：互动基础功能

### 3.1 评论入库

- 用户对文章发表评论。
- 评论保存到 MySQL。
- 支持查看文章评论列表。
- 评论后热度 +3。

### 3.2 点赞功能

- 用户点赞文章。
- 防止同一用户重复点赞。
- MySQL 保存点赞记录。
- 文章点赞数增加。
- Redis ZSET 热度 +5。
- `article_like` 表中设置 `user_id + article_id` 唯一索引。

## 阶段 4：核心架构功能开发

### 4.1 网关动态限流

实现目标：

- 限制某 IP 对文章详情接口的访问频率。
- 示例：同一 IP 每 10 秒最多访问文章详情接口 20 次。
- 超过限制返回 429。

推荐实现方式：

- 在 Gateway 中使用 Redis 计数。
- Key：`rate_limit:{ip}:{path}`。
- 设置过期时间：10 秒。
- Redis Hash 保存限流配置：`rate_limit_config:article_detail`。

配置字段：

```text
windowSeconds = 10
maxRequests = 20
enabled = true
```

管理员接口：

```text
PUT /api/admin/rate-limit/article-detail
```

报告说明重点：网关每次处理请求时从 Redis 读取当前限流配置，配置修改后无需重启服务即可生效。

### 4.2 Redis ZSET 实时热榜

数据结构：

```text
article:hot:ranking
member = articleId
score = hotScore
```

热度规则：

- 阅读文章：+1。
- 发布文章：+2。
- 评论文章：+3。
- 点赞文章：+5。

获取 Top10：

```text
ZREVRANGE article:hot:ranking 0 9 WITHSCORES
```

之后根据 articleId 批量查询文章标题、作者、摘要等信息。

### 4.3 长文异步广播处理

业务场景：用户发布长文后，不能让发布接口一直等待 AI 处理完成。

正确链路：

```text
发布文章成功 -> 发送 MQ 消息 -> 立即返回发布成功
```

下游消费者异步处理：

- AI 标签提取消费者。
- AI 合规检测消费者。

RabbitMQ 模式建议：Fanout Exchange。

队列结构：

```text
article.publish.exchange
├── article.tag.queue
└── article.audit.queue
```

失败处理建议：

- 消费失败打印错误日志。
- 手动 ACK。
- 消费失败进入重试队列。
- 多次失败进入死信队列。

### 4.4 AI 流式创作辅助

实现目标：

- 用户输入一段提示词。
- 后端不是一次性返回完整结果，而是逐字或逐句返回。
- 使用 SSE。

本地演示方式：

- 可以使用 Mock Streaming。
- 真实 API 不稳定时，报告说明本地原型支持 Mock，生产环境可替换为真实 LLM API。

## 阶段 5：测试、文档和演示

- 整理 Swagger / OpenAPI 文档。
- 整理 Postman / Apifox Collection。
- 准备 3-5 分钟演示视频。
- 完成项目报告。
- 明确小组分工。

演示流程建议：

1. 注册。
2. 登录。
3. 创建草稿。
4. 发布文章。
5. RabbitMQ 消费者处理文章事件。
6. 标签和合规检测结果保存。
7. 查看文章详情。
8. 点赞、评论。
9. 查看 Redis ZSET 热榜 Top10。
10. 测试 AI SSE 流式续写。
11. 展示网关限流效果。
12. 展示 Swagger / OpenAPI 文档。

