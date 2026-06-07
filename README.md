# AI-KnowledgeHub

基于 AI 的智能知识库与内容发布平台。

本项目定位为一个"带 AI 能力的知乎后台系统"原型，重点展示微服务拆分、网关鉴权与限流、Redis 热榜、RabbitMQ 异步广播、AI 流式响应和生产环境架构思考。

## 项目目标

- 用户注册、登录、注销和个人信息查询。
- 用户创建文章草稿、修改文章、发布文章、逻辑删除文章、查询最新文章和文章详情。
- 用户评论文章、点赞文章，并防止重复点赞。
- 阅读、点赞、评论、发布行为实时更新 Redis ZSET 热榜。
- 发布文章后发送 RabbitMQ 广播消息，由 AI 标签提取和 AI 合规检测消费者异步处理。
- 支持 AI 续写接口，其中流式续写通过 SSE 逐字或逐句返回。
- 通过 Spring Cloud Gateway 统一入口、JWT 鉴权和动态限流。

## 推荐技术栈

- 后端框架：Spring Boot 3.2.5、Spring Cloud 2023.0.1、Spring Cloud Gateway（WebFlux）。
- 数据库：MySQL 8.0（生产）/ H2（本地 dev）。
- 缓存与热榜：Redis 7，使用 ZSET、缓存和限流计数。
- 消息队列：RabbitMQ 3，使用 Fanout Exchange 广播文章发布事件。
- ORM：MyBatis-Plus 3.5.5。
- API 文档：Knife4j 4.4 + OpenAPI 3。
- AI 接口：OpenAI 兼容接口、通义、智谱或 Mock LLM。
- 测试工具：Postman / Apifox。

## 目录结构

```text
ai-knowledgehub/
├── gateway-service/      # 网关（端口 8080，WebFlux）
├── user-service/         # 用户（端口 8081）
├── article-service/      # 文章（端口 8082）
├── ranking-service/      # 热榜（端口 8083）
├── ai-service/           # AI（端口 8084，由 C 负责）
├── common/               # 公共模块（统一返回、异常、JWT、MyBatis-Plus 配置、Swagger、Page）
├── docs/                 # 项目文档与数据库脚本
├── postman/              # Postman 测试集
├── docker-compose.yml    # MySQL + Redis + RabbitMQ
├── .env.example          # 环境变量示例
└── README.md
```

## 服务拆分

| 服务 | 端口 | 负责人 | 主要职责 |
| --- | --- | --- | --- |
| gateway-service | 8080 | A | 统一入口、路由转发、JWT 校验、角色权限判断、IP 限流、动态限流参数读取 |
| user-service | 8081 | A | 用户注册、登录、注销、个人信息、角色识别、Token 黑名单（加分项） |
| article-service | 8082 | B | 文章草稿、修改、发布、逻辑删除、列表、详情、评论、点赞 |
| ranking-service | 8083 | B | Redis ZSET 文章热榜、阅读/点赞/评论热度更新、Top10 查询 |
| ai-service | 8084 | C | AI 续写、SSE 流式响应、MQ 消费、标签提取、合规检测、AI 结果保存 |
| common | - | A | 统一返回结构、错误码、异常处理、JWT 工具、MyBatis-Plus 配置、Swagger、Page、SecurityUtils |

## 已创建文档

- `docs/A/成员A开发计划.md` — 成员 A 的开发计划。
- `docs/A/各阶段审查与验收方案.md` — 6 阶段详细审查与验收方案。
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

### 1. 环境要求

| 工具 | 最低版本 | 说明 |
| --- | --- | --- |
| JDK | 17 | 强制要求，被 `maven-enforcer-plugin` 校验 |
| Maven | 3.8.0 | 强制要求，被 `maven-enforcer-plugin` 校验 |
| Docker | 20.10 | 启动 MySQL/Redis/RabbitMQ |
| Docker Compose | v2 | 启动中间件编排 |

### 2. 启动基础设施

```bash
# 复制环境变量（可选）
cp .env.example .env

cd ai-knowledgehub
docker-compose up -d
```

启动的服务：
- MySQL 8.0（端口 3306，自动执行 `docs/database-init.sql`）
- Redis 7（端口 6379，可选密码）
- RabbitMQ 3（端口 5672，管理界面 15672）

健康检查：
```bash
docker ps
# 期望看到 3 个容器均显示 "healthy"
```

### 3. 初始化数据库

`docs/database-init.sql` 已挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/`，**首次启动自动执行**。如需手动重新初始化：

```bash
docker exec -i akh-mysql mysql -uroot -proot123456 < docs/database-init.sql
```

### 4. 编译项目

```bash
mvn clean install -DskipTests
```

如果出现 `requireJavaVersion` 报错，说明 JDK 版本不对；如果出现 `dependencyConvergence` 报错，说明有依赖版本冲突，请联系 A。

### 5. 启动服务（按顺序）

```bash
# 1. 启动 user-service
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &

# 2. 启动 article-service
cd article-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &

# 3. 启动 ranking-service
cd ranking-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &

# 4. 启动 ai-service（C 负责）
cd ai-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev &

# 5. 最后启动 gateway-service（最关键）
cd gateway-service && mvn spring-boot:run &

# 6. 验证健康
curl http://localhost:8080/actuator/health
# 期望：{"status":"UP"}
```

### 6. 访问接口

- 网关地址：`http://localhost:8080`
- Swagger UI：每个服务独立 `http://localhost:8081/doc.html` 等
- RabbitMQ 管理：`http://localhost:15672`（guest / guest）
- H2 控制台（dev）：`http://localhost:8081/h2-console`

### 7. 测试接口

导入 `postman/AI-KnowledgeHub.postman_collection.json` 到 Postman 进行测试。

## 常见问题（FAQ）

### Q1：`mvn install` 报 `requireJavaVersion` 错误

A：`pom.xml` 中 `maven-enforcer-plugin` 强制要求 JDK 17+。请检查：
```bash
java -version  # 应显示 17.x
mvn -v         # Maven 使用的 Java 应为 17
```

### Q2：MySQL 容器启动后 `docker ps` 显示 unhealthy

A：MySQL 启动较慢（首次约 30s）。等待 30s 后再查 `docker ps`。
如仍 unhealthy：
```bash
docker logs akh-mysql | tail -20
```

### Q3：服务启动后访问 8080 网关返回 502

A：下游服务还没启动，或服务注册失败。检查：
```bash
docker logs akh-mysql  # MySQL 状态
docker logs akh-redis   # Redis 状态
```
确认 3 个中间件都 healthy 后，再启动下游服务。

### Q4：H2 控制台看不到 `user` 表

A：`user-service` 启动时如果使用 dev profile，会自动用 H2。表结构由 MyBatis-Plus 启动时根据实体类自动创建。如未自动建表，请检查 `@MapperScan` 路径是否包含 `com.ai.knowledgehub.user.mapper`。

### Q5：JWT 报 `SignatureException`

A：JWT 密钥不一致。网关（gateway-service）和 user-service 必须用同一个 `JWT_SECRET` 环境变量。当前默认密钥在两个 application.yml 中都已硬编码，开发环境一致即可。

### Q6：端口冲突

A：服务端口固定如下，发现冲突请修改对应 application.yml：
| 服务 | 端口 |
| --- | --- |
| gateway-service | 8080 |
| user-service | 8081 |
| article-service | 8082 |
| ranking-service | 8083 |
| ai-service | 8084 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 |

### Q7：knife4j/swagger 报 404

A：检查 `application.yml` 中 `swagger.enabled` 是否为 true（默认 true）。网关（gateway-service）默认关闭 swagger（只做聚合）。

## 模块开发状态

| 模块 | 状态 | 负责人 | 备注 |
| --- | --- | --- | --- |
| common | ✅ 阶段 1 完成 | A | 统一返回、异常、JWT、MyBatis-Plus 配置、Swagger、Page、SecurityUtils、AuthException |
| docker-compose | ✅ 阶段 1 完成 | A | MySQL/Redis/RabbitMQ + healthcheck + 挂载初始化脚本 |
| 根 pom.xml | ✅ 阶段 1 完成 | A | 5 个 module + enforcer 插件 + UTF-8 + 依赖收敛 |
| user-service 骨架 | ✅ 阶段 1 完成 | A | 阶段 2 继续完善业务 |
| gateway-service 骨架 | ✅ 阶段 1 完成 | A | 阶段 3 继续完善 Filter |
| article-service | ✅ 已完成 | B | CRUD、评论、点赞、MQ 消息 |
| ranking-service | ✅ 已完成 | B | Redis ZSET 热度计算、Top10 |
| database-init.sql | ✅ 已完成 | B | 数据库初始化脚本 |
| Postman 测试集 | ✅ 已完成 | B/C | API 测试集合 |
| ai-service | 🚧 计划中 | C | 阶段 1 完成后 C 启动开发 |
