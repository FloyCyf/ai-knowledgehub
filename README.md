# AI-KnowledgeHub

基于 AI 的智能知识库与内容发布平台。

本项目定位为一个“带 AI 能力的知乎后台系统”原型，重点展示微服务拆分、网关鉴权与限流、Redis 热榜、RabbitMQ 异步广播、AI 流式响应和生产环境架构思考。

## 项目目标

- 用户注册、登录、注销和个人信息查询。
- 用户创建文章草稿、修改文章、发布文章、逻辑删除文章、查询最新文章和文章详情。
- 用户评论文章、点赞文章，并防止重复点赞。
- 阅读、点赞、评论、发布行为实时更新 Redis ZSET 热榜。
- 发布文章后发送 RabbitMQ 广播消息，由 AI 标签提取和 AI 合规检测消费者异步处理。
- 支持 AI 续写接口，其中流式续写通过 SSE 逐字或逐句返回。
- 通过 Spring Cloud Gateway 统一入口、JWT 鉴权和动态限流。

## 推荐技术栈

- 后端框架：Spring Boot、Spring Cloud Gateway。
- 数据库：MySQL。
- 缓存与热榜：Redis，使用 ZSET、缓存和限流计数。
- 消息队列：RabbitMQ，使用 Fanout Exchange 广播文章发布事件。
- API 文档：Swagger / Knife4j / OpenAPI。
- AI 接口：OpenAI 兼容接口、通义、智谱或 Mock LLM。
- 测试工具：Postman / Apifox。

## 目录结构

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

## 服务拆分

| 服务 | 主要职责 |
| --- | --- |
| gateway-service | 统一入口、路由转发、JWT 校验、角色权限判断、IP 限流、动态限流参数读取 |
| user-service | 用户注册、登录、注销、个人信息、角色识别 |
| article-service | 文章草稿、修改、发布、逻辑删除、列表、详情、评论、点赞 |
| ranking-service | Redis ZSET 文章热榜、阅读/点赞/评论热度更新、Top10 查询 |
| ai-service | AI 续写、SSE 流式响应、MQ 消费、标签提取、合规检测、AI 结果保存 |
| common | 统一返回结构、错误码、异常处理、JWT 工具、通用 DTO |

## 已创建文档

- `docs/project-overview.md`：项目说明与验收目标。
- `docs/architecture.md`：系统架构、微服务职责和核心业务链路。
- `docs/api-spec.md`：接口规范说明。
- `docs/api-openapi.yaml`：OpenAPI 草案。
- `docs/database-design.md`：数据库表设计建议。
- `docs/database-init.sql`：数据库初始化脚本。
- `docs/task-list.md`：按开发阶段拆分的任务清单。
- `docs/coding-standards.md`：代码规范与统一约定。
- `docs/deployment-notes.md`：本地环境和生产环境说明。

## 快速启动

### 1. 启动基础设施

```bash
cd ai-knowledgehub
docker-compose up -d
```

启动的服务：
- MySQL 8.0 (端口 3306)
- Redis 7 (端口 6379)
- RabbitMQ 3 (端口 5672, 管理界面 15672)

### 2. 初始化数据库

连接 MySQL 执行 `docs/database-init.sql`：

```bash
mysql -h localhost -u root -p root123456 ai_knowledgehub < docs/database-init.sql
```

### 3. 编译项目

```bash
mvn clean install -DskipTests
```

### 4. 启动服务

```bash
# 启动 article-service (端口 8082)
cd article-service
mvn spring-boot:run

# 启动 ranking-service (端口 8083)
cd ranking-service
mvn spring-boot:run
```

### 5. 测试接口

导入 `postman/AI-KnowledgeHub.postman_collection.json` 到 Postman 进行测试。

## 组员 B 负责模块

| 模块 | 状态 | 说明 |
|------|------|------|
| common | ✅ 已完成 | 公共模块：统一返回、异常处理、JWT工具 |
| article-service | ✅ 已完成 | 文章服务：CRUD、评论、点赞、MQ消息 |
| ranking-service | ✅ 已完成 | 热榜服务：Redis ZSET 热度计算、Top10 |
| database-init.sql | ✅ 已完成 | 数据库初始化脚本 |
| Postman 测试集 | ✅ 已完成 | API 测试集合 |

### 核心接口

**article-service (端口 8082)**
- `POST /api/articles/draft` - 创建草稿
- `PUT /api/articles/{id}` - 修改文章
- `POST /api/articles/{id}/publish` - 发布文章
- `DELETE /api/articles/{id}` - 删除文章
- `GET /api/articles/latest` - 文章列表
- `GET /api/articles/{id}` - 文章详情
- `POST /api/articles/{id}/comments` - 发表评论
- `GET /api/articles/{id}/comments` - 评论列表
- `POST /api/articles/{id}/like` - 点赞文章

**ranking-service (端口 8083)**
- `GET /api/ranking/top10` - 热榜 Top10
- `POST /api/ranking/articles/{id}/view` - 阅读热度+1
- `POST /api/ranking/articles/{id}/like` - 点赞热度+5
- `POST /api/ranking/articles/{id}/comment` - 评论热度+3
- `POST /api/ranking/articles/{id}/publish` - 发布热度+2

### 热度规则

| 行为 | 热度增量 |
|------|----------|
| 阅读文章 | +1 |
| 发布文章 | +2 |
| 评论文章 | +3 |
| 点赞文章 | +5 |

