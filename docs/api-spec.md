# 接口规范文档

## 基础约定

### 统一前缀

所有外部接口统一通过网关访问，路径以 `/api` 开头。

### 统一返回格式

普通 JSON 接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 常用错误码

| code | 含义 |
| --- | --- |
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 Token 无效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 409 | 数据冲突，例如重复点赞 |
| 429 | 访问过于频繁 |
| 500 | 服务内部错误 |

### JWT Header

登录后的请求统一携带：

```http
Authorization: Bearer <token>
```

网关解析 Token 后，将用户上下文透传给后端服务：

```http
X-User-Id: 12
X-User-Role: USER
```

### 分页参数

分页列表统一使用：

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| page | integer | 1 | 页码，从 1 开始 |
| size | integer | 10 | 每页数量 |

### 时间格式

统一使用：

```text
yyyy-MM-dd HH:mm:ss
```

示例：

```text
2026-06-02 20:00:00
```

## 用户接口

### 注册

`POST /api/user/register`

请求体：

```json
{
  "username": "alice",
  "password": "123456"
}
```

说明：

- 用户名唯一校验。
- 密码加密存储。
- 默认角色为普通用户。
- 支持管理员角色初始化。

### 登录

`POST /api/user/login`

请求体：

```json
{
  "username": "alice",
  "password": "123456"
}
```

响应 data：

```json
{
  "token": "jwt-token",
  "user": {
    "id": 12,
    "username": "alice",
    "role": "USER"
  }
}
```

### 注销

`POST /api/user/logout`

说明：

- 简单版：前端丢弃 Token。
- 加分版：Redis 维护 Token 黑名单，实现注销后 Token 失效控制。

### 个人信息

`GET /api/user/profile`

需要登录。

## 文章接口

### 创建文章草稿

`POST /api/articles/draft`

请求体：

```json
{
  "title": "Redis ZSET 实现热榜",
  "content": "文章正文...",
  "summary": "文章摘要..."
}
```

说明：

- 登录用户创建草稿。
- 文章状态为 `DRAFT`。
- 保存标题、正文、摘要和作者 ID。

### 修改文章

`PUT /api/articles/{id}`

说明：

- 作者本人可以修改。
- 管理员可以修改。
- 已删除文章不能修改。

### 发布文章

`POST /api/articles/{id}/publish`

说明：

- 状态从 `DRAFT` 改为 `PUBLISHED`。
- 设置发布时间。
- 发送 MQ 消息 `ArticlePublishedEvent`。
- 发布热度建议 +2。

MQ 消息示例：

```json
{
  "articleId": 1001,
  "title": "Redis ZSET 实现热榜",
  "authorId": 12,
  "content": "文章正文...",
  "publishTime": "2026-06-02 20:00:00"
}
```

### 逻辑删除文章

`DELETE /api/articles/{id}`

说明：

- 修改 `deleted` 字段。
- 不物理删除数据库记录。

### 分页获取最新文章

`GET /api/articles/latest?page=1&size=10`

说明：

- 只返回已发布、未删除文章。
- 按发布时间倒序。
- 支持分页。

### 获取文章详情

`GET /api/articles/{id}`

说明：

- 查询文章详情。
- 阅读量增加。
- Redis 热度 +1。
- 可选：文章详情缓存。

### 评论文章

`POST /api/articles/{id}/comments`

请求体：

```json
{
  "content": "写得很清楚"
}
```

说明：

- 评论保存到 MySQL。
- 支持查看文章评论列表。
- 评论后热度 +3。

### 查看评论列表

`GET /api/articles/{id}/comments?page=1&size=10`

### 点赞文章

`POST /api/articles/{id}/like`

说明：

- 用户点赞文章。
- 防止同一用户重复点赞。
- MySQL 保存点赞记录。
- `article.like_count +1`。
- Redis 热度 +5。
- `article_like` 表中设置 `user_id + article_id` 唯一索引。

## 热榜接口

### 记录阅读热度

`POST /api/ranking/articles/{id}/view`

说明：文章阅读热度 +1。

### 记录点赞热度

`POST /api/ranking/articles/{id}/like`

说明：文章点赞热度 +5。

### 获取 Top10

`GET /api/ranking/top10`

说明：

- 从 Redis ZSET 查询前 10 名。
- 根据 articleId 补充文章标题、作者、摘要等信息。

Redis 命令示例：

```text
ZREVRANGE article:hot:ranking 0 9 WITHSCORES
```

## AI 接口

### AI 续写

`POST /api/ai/continue-writing`

请求体：

```json
{
  "prompt": "请继续写一段关于 Redis 热榜设计的内容"
}
```

### AI 流式续写

`GET /api/ai/continue-writing/stream?prompt=xxx`

响应头：

```http
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

响应示例：

```text
data: Redis ZSET 很适合实现排行榜场景...
data: 因为它可以根据 score 进行排序...
```

说明：

- 后端不是一次性返回完整结果，而是逐字或逐句返回。
- 本地可用 Mock Streaming，生产环境替换为真实 LLM Streaming API。

### 获取文章 AI 分析结果

`GET /api/ai/articles/{id}/analysis`

返回文章标签提取和合规检测结果。

## 管理员接口

### 修改文章详情限流配置

`PUT /api/admin/rate-limit/article-detail`

请求体：

```json
{
  "windowSeconds": 10,
  "maxRequests": 20,
  "enabled": true
}
```

说明：

- 网关每次处理请求时从 Redis 读取当前限流配置。
- 配置修改后无需重启服务即可生效。

限流错误响应：

```json
{
  "code": 429,
  "message": "访问过于频繁，请稍后再试"
}
```

