# 项目说明

## 项目名称

AI-KnowledgeHub：基于 AI 的智能知识库与内容发布平台。

## 核心理解

本项目不是追求功能数量最多的内容平台，而是用较清晰的业务功能证明后端系统能力：

- 微服务拆分。
- Spring Cloud Gateway 统一入口。
- JWT 鉴权和管理员权限识别。
- Redis ZSET 实时热榜。
- Redis 动态限流。
- RabbitMQ 异步广播处理。
- AI 标签提取、AI 合规检测和 AI 续写。
- SSE 流式响应。
- 本地可运行，报告可讲清架构设计。

## 最小可交付版本

至少完成以下内容：

1. 用户注册、登录、注销、个人信息查询。
2. 文章草稿、修改、发布、逻辑删除、列表、详情。
3. 评论和点赞功能。
4. Redis ZSET 热榜 Top10。
5. RabbitMQ 发布文章事件，并由消费者接收。
6. AI 标签提取、合规检测和流式续写接口，真实 API 不稳定时可使用 Mock。
7. Swagger / OpenAPI 文档。
8. Postman / Apifox 测试集。
9. Docker Compose 启动 MySQL、Redis、RabbitMQ。
10. README、项目报告和小组分工说明。

## 推荐实现边界

ranking 功能可以在实现上放入 article-service，但报告中建议把热榜模块独立描述。更推荐保留 ranking-service，使微服务架构更清晰。

AI 接口可以先使用 Mock LLM，本地原型稳定后再替换为真实 LLM API。

