# 本地环境与生产环境说明

## 本地环境

最低本地依赖：

- MySQL。
- Redis。
- RabbitMQ。

可以使用项目根目录的 `docker-compose.yml` 启动基础中间件。

RabbitMQ 管理页面：

```text
http://localhost:15672
```

默认账号密码：

```text
guest / guest
```

## 本地服务启动建议

推荐先按顺序启动：

1. MySQL、Redis、RabbitMQ。
2. user-service。
3. article-service。
4. ranking-service。
5. ai-service。
6. gateway-service。

所有外部请求通过 gateway-service 访问。

## 生产环境架构说明

报告中建议重点说明：

- LLM 调用响应慢、费用高、并发受限，不能把所有 AI 请求都直接打到第三方 API。
- 可以使用请求排队、限流、结果缓存、相似 Prompt 缓存、异步任务队列和降级策略优化 AI 调用链路。
- 网关限流可以保护后端服务和数据库。
- Redis ZSET 可以减少 MySQL 排序统计压力。
- RabbitMQ 将耗时 AI 处理从发布接口中解耦。
- SSE 适合长文本生成过程中的渐进式返回。

## 高可用和优化方向

- 网关多实例部署。
- 后端服务多实例部署。
- Redis 主从或集群。
- MySQL 索引优化和读写分离。
- RabbitMQ 消息确认、重试队列和死信队列。
- AI 调用降级到 Mock 或缓存结果。

