# 代码规范与统一约定

## 模块边界

- `gateway-service` 只处理网关、鉴权、限流和路由，不承载文章业务。
- `user-service` 只处理用户身份和用户资料。
- `article-service` 负责文章、评论、点赞等内容业务。
- `ranking-service` 负责热榜分数维护和 Top10 查询。
- `ai-service` 负责 AI 调用、MQ 消费和 AI 处理结果。
- `common` 存放各服务共享的通用结构和工具。

## 统一返回结构

建议在 `common` 中定义统一响应对象：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 统一异常处理

建议按错误类型统一封装：

- 参数错误：400。
- 未登录：401。
- 无权限：403。
- 数据不存在：404。
- 数据冲突：409。
- 限流：429。
- 系统异常：500。

## 鉴权约定

- 对外请求统一携带 `Authorization: Bearer <token>`。
- 网关负责校验 Token。
- 网关向后端服务透传 `X-User-Id` 和 `X-User-Role`。
- 管理员接口必须校验 `X-User-Role=ADMIN`。

## 数据写入约定

- 删除文章和评论使用逻辑删除，不直接物理删除。
- 点赞表必须设置 `article_id + user_id` 唯一约束。
- 发布文章时必须写入发布时间。
- AI 处理结果单独落表，避免阻塞文章发布流程。

## Redis 使用约定

- 热榜使用 ZSET：`article:hot:ranking`。
- 限流计数使用短 TTL Key：`rate_limit:{ip}:{path}`。
- 限流配置使用 Redis Hash：`rate_limit_config:article_detail`。
- 注销登录加分项使用 Token 黑名单。

## RabbitMQ 使用约定

- 文章发布事件使用 Fanout Exchange。
- Exchange：`article.publish.exchange`。
- 标签队列：`article.tag.queue`。
- 合规检测队列：`article.audit.queue`。
- 消费者之间互不影响。
- 建议开启手动 ACK，并补充重试队列和死信队列说明。

## AI 调用约定

- 本地原型优先支持 Mock LLM。
- 真实 LLM API 通过配置切换。
- 流式续写使用 SSE，响应类型为 `text/event-stream`。
- 生产环境需要考虑请求排队、限流、结果缓存、相似 Prompt 缓存、异步任务队列和降级策略。

