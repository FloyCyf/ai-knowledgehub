# 成员 A 开发计划 — 网关 / 用户 / 项目架构

> 项目：AI-KnowledgeHub（基于 AI 的智能知识库与内容发布平台）
> 角色：成员 A（后端主架构 / 网关 / 用户认证 / 项目骨架负责人）
> 文档生成时间：2026-06-07
> 文档位置：`docs/A/成员A开发计划.md`

---

## 一、项目总览与项目目标

### 1.1 项目核心目标

按《项目分析以及分工.pdf》的定义，本项目不是要做一个功能丰富的知乎，而是要证明我们掌握了以下 7 项后端核心能力：

1. **微服务拆分**（5 个独立服务）
2. **网关统一入口与动态限流**（Spring Cloud Gateway + Redis）
3. **Redis ZSET 热榜**
4. **消息队列异步处理**（RabbitMQ Fanout）
5. **AI 大模型集成**（SSE 流式 + 异步消费）
6. **SSE / Streaming 流式返回**
7. **生产环境高并发、高可用设计**

### 1.2 整体架构（5 个微服务）

```
用户/Postman ──► gateway-service (8080) ──► user-service (8081)
                                       ──► article-service (8082)
                                       ──► ranking-service (8083)
                                       ──► ai-service (8084)
                                       ──► 共享 MySQL/Redis/RabbitMQ
```

### 1.3 微服务职责边界

| 服务 | 端口 | 负责人 | 核心职责 |
| --- | --- | --- | --- |
| gateway-service | 8080 | **A** | 统一入口、JWT 鉴权、IP 限流、动态限流、路由 |
| user-service | 8081 | **A** | 注册/登录、JWT 签发、用户资料、角色管理 |
| article-service | 8082 | B | 文章 CRUD、评论、点赞、阅读量、MQ 发布事件 |
| ranking-service | 8083 | B | Redis ZSET 维护热度、Top10 查询 |
| ai-service | 8084 | C | AI 续写（SSE）、MQ 消费（标签/合规）、AI 结果落库 |

### 1.4 成员 A 的职责范围（来自 PDF 第 22~24 页）

> **成员 A：网关、用户认证、项目架构负责人**

详细任务：

- **A-1 项目初始化**（项目骨架、common、公共依赖、统一返回、统一异常、Swagger 基础配置）
- **A-2 user-service**（注册 / 登录 / 注销 / 个人信息）
- **A-3 gateway-service**（路由 / JWT 校验 / 角色透传）
- **A-4 网关动态限流**（Redis 计数 + 动态配置读取 + 管理员修改接口）
- **A-5 报告负责章节**（1.项目概述、2.系统总体设计、5.1、5.4、8.1、8.4、9.1）

---

## 二、当前开发进度评估

### 2.1 已有产物（已完成）

| 模块 | 状态 | 说明 |
| --- | --- | --- |
| 根 `pom.xml` | ✅ | 已建立 Spring Boot 3.2.5 + Spring Cloud 2023.0.1 父工程 |
| `common` 模块 | ✅ | `ApiResponse` / `ResultCode` / `JwtUtil` / `BaseEntity` / `BusinessException` / `GlobalExceptionHandler` / `HeaderConstants` |
| `article-service` | ✅（B） | Controller/Service/Mapper/Entity/DTO/VO 全套，RabbitMQ Fanout 已发布 |
| `ranking-service` | ✅（B） | Redis ZSET Top10 查询 + view/like/comment/publish 4 个加分接口 |
| `docs/api-spec.md`、`docs/architecture.md`、`docs/coding-standards.md`、`docs/database-design.md` | ✅ | 规范已发布 |
| `docs/database-init.sql` | ✅ | MySQL 全量建表脚本（user / article / comment / article_like / article_ai_tag / article_audit_result）|
| `docs/h2-init.sql` | ✅ | 本地 H2 模式脚本 |
| `docker-compose.yml` | ✅（基础版） | MySQL + Redis + RabbitMQ |
| `postman/AI-KnowledgeHub.postman_collection.json` | ✅ | Postman 集 |

### 2.2 成员 A 待办（尚未开工）

| 任务 | 状态 | 紧急度 |
| --- | --- | --- |
| `gateway-service` 模块（pom + 启动类 + 配置） | ❌ 未开始 | ⭐⭐⭐ |
| `user-service` 模块（pom + 启动类 + Controller/Service/Mapper/Entity/DTO/VO） | ❌ 未开始 | ⭐⭐⭐ |
| 网关统一鉴权 GlobalFilter（JWT 解析 + 角色透传） | ❌ 未开始 | ⭐⭐⭐ |
| 网关动态限流 GlobalFilter（Redis 计数 + 动态配置） | ❌ 未开始 | ⭐⭐⭐ |
| 管理员修改限流配置接口 `PUT /api/admin/rate-limit/article-detail` | ❌ 未开始 | ⭐⭐ |
| 路由转发配置（5 服务路由 + 路径前缀） | ❌ 未开始 | ⭐⭐⭐ |
| Swagger/Knife4j 聚合文档 | ❌ 未开始 | ⭐⭐ |
| 报告章节撰写 | ❌ 未开始 | ⭐⭐ |

### 2.3 对 B 已有代码的合规性审查

我在通读 B 的代码后，发现 **A 需要主动推动 B 修正以下几个合规问题**，否则会影响联调：

#### 问题 1：B 的 Controller 没有使用 common 统一返回结构 ❌严重

`article-service` 的所有 Controller（`ArticleController`、`CommentController`、`LikeController`）都自己用 `Map<String,Object>` 拼装 `code/message/data`，没有返回 `ApiResponse<T>`。这违反了 `docs/coding-standards.md` 第 12~21 行"统一返回结构"的约定。

**修正要求**：B 必须改用 `ApiResponse.success(...)` / `ApiResponse.fail(...)`，并由 `GlobalExceptionHandler` 统一处理异常。**A 需要把这个约定写进群规并在评审时把关。**

#### 问题 2：B 的 Controller 自行 try-catch RuntimeException，绕过了 GlobalExceptionHandler ❌严重

B 在每个 Controller 中 `try { ... } catch (RuntimeException e) { return ResponseEntity.status(401).body(...); }`，**完全绕过了 common 模块的 `GlobalExceptionHandler`**。这意味着：

- 业务异常没有走 `BusinessException`，错误码不统一
- 校验异常、权限异常全部错乱

**A 的应对**：
1. 在 `common` 里再写一个 `AuthException extends BusinessException`，并把 401/403 错误码补齐
2. 推动 B 把所有 `try-catch` 删掉，统一抛 `BusinessException`
3. 网关返回 401/403 时，统一返回 `ApiResponse` 结构（重要！）

#### 问题 3：B 在 `article-service` 中又写了一个 `RankingService`，与 `ranking-service` 职责重叠 ⚠️ 中等

`article-service/src/.../service/RankingService.java` 自己又用 `RedisTemplate` 维护了一份热榜数据，与 `ranking-service` 的实现是双份。这会导致两边 Redis 行为不一致（一个是 ZINCRBY 一个是 incrementScore 行为相似，但 member 类型不同：article-service 用 `Long` 作为 member，ranking-service 用 `String`）。**A 需要在群里推动 B 把热榜写入统一收口到 `ranking-service`，article-service 通过 HTTP 调用或 MQ 通知。**

#### 问题 4：B 的 `Article` 实体没有继承 `BaseEntity` ⚠️ 轻

`common/BaseEntity` 已定义 `id / createTime / updateTime / createBy / updateBy / deleted`，但 B 重新在 `Article` 里声明了 `id/createTime/updateTime/deleted`，且**没有用 `@TableField(fill = ...)` 自动填充**——B 是手动在 Service 里 `setCreatedAt(LocalDateTime.now())`。这会导致：

- createBy / updateBy 无法自动填
- 全员需要手工 `setXxxTime`，容易漏

**A 的建议**：推动 B 把 `Article` 改成 `extends BaseEntity`，在 `common` 加 `MybatisPlusConfig`（其实 B 已加了 `MybatisPlusConfig` 但未注册 `MetaObjectHandler`），由框架自动填充。

#### 问题 5：B 的 `RankingService` 返回的 `HotArticleVO` 缺少文章标题/作者 ⚠️ 中等

`docs/architecture.md` 第 215 行要求 Top10 返回时"根据 articleId 批量查询文章标题、作者、摘要等信息"。B 当前只返回 `articleId + hotScore`，未补全文章信息。

**A 的推动**：让 B 增加一次到 `article-service` 的内部 HTTP 调用（FeignClient 或 RestTemplate），按 articleId 列表批量查询标题/作者/摘要。B 当前是独立服务，建议用 OpenFeign。

#### 问题 6：根 `pom.xml` 的 `<modules>` 缺少 `gateway-service` / `user-service` / `ai-service` ❌ 紧急

当前根 `pom.xml:14-18` 只列了 `common / article-service / ranking-service`。A 创建 `gateway-service` 和 `user-service` 的 pom 后，**必须立即**把这两个 module 加到根 pom，否则整个工程无法 `mvn install` 编译通过。

#### 问题 7：`docker-compose.yml` 缺少 Redis 密码和 RabbitMQ 健康检查 ⚠️ 轻

当前 docker-compose 启动 MySQL/Redis/RabbitMQ 但没有健康检查和初始数据库 schema 注入。**A 需要在 docker-compose.yml 里：**

- 用 `volumes` 挂载 `docs/database-init.sql` 到 `/docker-entrypoint-initdb.d/`
- 给 Redis 加可选密码
- 加 healthcheck 避免服务启动时 Redis/MQ 还没就绪

---

## 三、成员 A 详细开发计划（共 6 个阶段）

### 阶段 1：项目骨架补齐与公共能力增强（Day 1）⏱ ~3 小时

> 目标：把根 pom 修好，让 A/B/C 三方都能编译。

#### 1.1 根 `pom.xml` 增加 gateway / user 模块声明
- 把 `gateway-service` 和 `user-service` 加到 `<modules>`
- 暂不加 `ai-service`（C 自己负责），等 C 提 pr 后再加

#### 1.2 完善 `common` 模块的鉴权能力
- 新增 `exception/AuthException.java`（继承 `BusinessException`）
- 在 `ResultCode` 中确认 `TOKEN_INVALID=2007`、`TOKEN_EXPIRED=2008` 已存在（已存在 ✅）
- 新增 `PageRequest`、`PageResult`（统一分页 DTO）
- 新增 `security/SecurityUtils.java`（从 SecurityContextHolder / ThreadLocal 取 X-User-Id）
- 新增 `config/MybatisPlusConfig.java` 移到 common，并加 `MetaObjectHandler` 实现自动填充

#### 1.3 Swagger/Knife4j 基础配置
- 在 `common` 加 `swagger/SwaggerConfig`（按 SpringDoc OpenAPI 3 + Knife4j）
- 通过 `application.yml` 统一配置 `springdoc.api-docs.path=/v3/api-docs`
- 给每个微服务单独暴露自己的 `/doc.html`，**网关侧通过 gateway 路由转发 `/webjars/**` 和 `/doc.html`**

#### 1.4 `docker-compose.yml` 完善
- 挂载 `docs/database-init.sql` 到 MySQL 自动初始化
- 加 `depends_on` + `healthcheck` 等待 MySQL/Redis/RabbitMQ 就绪
- 增加 `network` 配置

**交付物**：
- 根 `pom.xml` 包含 5 个 module
- common 多 3~4 个工具/异常类
- `docker-compose.yml` 自动初始化数据库

---

### 阶段 2：user-service 开发（Day 2~3）⏱ ~1 天

> 目标：让登录链路跑通：`注册 → 登录 → 返回 JWT → 后续接口带 token`

#### 2.1 基础工程搭建
1. 创建 `user-service/pom.xml`（参照 `article-service/pom.xml` 的依赖结构）
   - 依赖 `common`
   - 依赖 `spring-boot-starter-web` / `validation` / `data-jpa`（或 mybatis-plus）
   - 依赖 `mysql-connector-j` + `h2`（与 article-service 一致）
   - 依赖 `spring-boot-starter-data-redis`（注销用 Token 黑名单）
   - 依赖 `jjwt`（虽然 common 里有，但独立服务直接引用）
2. 创建 `user-service/src/main/java/com/ai/knowledgehub/user/UserApplication.java`
   ```java
   @SpringBootApplication(scanBasePackages = {"com.ai.knowledgehub.user", "com.ai.knowledgehub.common"})
   @MapperScan("com.ai.knowledgehub.user.mapper")
   public class UserApplication { public static void main(String[] args) { SpringApplication.run(UserApplication.class, args); } }
   ```
3. 创建 `application.yml`（端口 8081）
4. 复用 `common` 的 `DatabaseInitializer` 模式（建 user 表），或直接复用 `docs/database-init.sql`

#### 2.2 实体 / Mapper
1. `entity/User.java`（extends `BaseEntity`）：
   - `username` (unique, length 50)
   - `passwordHash` (length 255)
   - `role` (枚举 USER / ADMIN)
   - `status` (枚举 ENABLED / DISABLED)
2. `mapper/UserMapper.java` + `UserMapper.xml`（按 username 查询、按 id 查询）

#### 2.3 Service 层
1. `UserService.register(RegisterDTO)`：
   - 用户名唯一校验
   - BCrypt 加密密码（推荐 `BCryptPasswordEncoder`）
   - 默认 role=USER
   - 写库
2. `UserService.login(LoginDTO)`：
   - 校验 username + password
   - 调用 `JwtUtil.generateToken(userId, username, role)` 生成 token
   - 返回 `LoginVO { token, user: { id, username, role } }`
3. `UserService.logout(token)`：
   - 简单版：直接返回成功
   - 加分版：把 token 的 jti 写入 Redis 黑名单 `token:blacklist:{jti}`，TTL = token 剩余有效期
4. `UserService.getProfile(userId)`：
   - 返回 `UserVO { id, username, role, createdAt }`

#### 2.4 Controller
1. `controller/UserController.java`：
   - `POST /api/user/register` —— 接收 `RegisterDTO`（username, password）
   - `POST /api/user/login` —— 接收 `LoginDTO`
   - `POST /api/user/logout` —— 需要登录（网关已校验）
   - `GET /api/user/profile` —— 需要登录，从 header 拿 X-User-Id
2. 所有方法返回 `ApiResponse<T>`，**不自己拼 Map**
3. 业务异常抛 `BusinessException` + `AuthException`，由 `GlobalExceptionHandler` 兜底

#### 2.5 启动 `user-service` 验证
- `mvn install` 编译通过
- 启动后用 `curl POST /api/user/register` 创建一个用户
- 用 `curl POST /api/user/login` 拿 token
- 用 `curl -H "Authorization: Bearer <token>" GET /api/user/profile` 拿到用户信息

**交付物**：可独立启动的 `user-service`，4 个接口全部联调通过。

---

### 阶段 3：gateway-service 开发（Day 4~5）⏱ ~1.5 天

> 目标：所有外部请求走网关，网关统一鉴权，5 个微服务路由打通

#### 3.1 基础工程搭建
1. 创建 `gateway-service/pom.xml`：
   - 依赖 `spring-cloud-starter-gateway`（基于 WebFlux，不是 Spring MVC）
   - 依赖 `spring-boot-starter-data-redis`（限流计数）
   - 依赖 `common`（注意：common 用的是 Spring MVC 注解，gateway 是 WebFlux，**JwtUtil 必须自己写一个 WebFlux 兼容版本或放在 common 但只使用无 Web 依赖的部分**）
   - 依赖 `spring-cloud-starter-loadbalancer`（服务发现）
   - 依赖 `knife4j-openapi3-jakarta-spring-boot-starter`（仅用于聚合）
2. 创建 `GatewayApplication.java`
3. 创建 `application.yml`：
   - `server.port: 8080`
   - `spring.cloud.gateway.discovery.locator.enabled: true`（服务发现）
   - 路由列表（见 3.3）
4. **关键决策**：本项目暂不引入 Nacos，使用配置文件硬编码下游服务地址

#### 3.2 鉴权 GlobalFilter
1. `filter/AuthGlobalFilter.java`：
   - 拦截所有请求
   - 跳过白名单：`/api/user/register`、`/api/user/login`、`/api/admin/rate-limit/**`（仅 PUT 需要 role 校验）、`/doc.html`、`/v3/api-docs/**`、`/webjars/**`
   - 从 `Authorization: Bearer <token>` 拿 token
   - 调 `JwtUtil.parseToken(token)`，失败抛 `AuthException(UNAUTHORIZED)`
   - 把 `userId / username / role` 写入 `ServerWebExchange` 的 `mutate()` request header
   - 透传给下游服务：`X-User-Id` / `X-User-Role` / `X-User-Name`
2. `filter/AdminRoleFilter.java`（可选，也可以合并到 AuthFilter）：
   - 路径匹配 `/api/admin/**`
   - 检查 role == ADMIN，否则返回 403

#### 3.3 路由配置（`application.yml`）
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
        - id: article-service
          uri: lb://article-service
          predicates:
            - Path=/api/articles/**
        - id: ranking-service
          uri: lb://ranking-service
          predicates:
            - Path=/api/ranking/**
        - id: ai-service
          uri: lb://ai-service
          predicates:
            - Path=/api/ai/**
        - id: admin-routes
          uri: lb://user-service  # 或单独给 user-service 暴露管理员接口
          predicates:
            - Path=/api/admin/**
```

#### 3.4 启动验证
- `mvn install` 编译通过
- 启动 5 个服务，浏览器访问 `http://localhost:8080/api/user/register`
- 网关路由成功
- 拿 token 后访问 `http://localhost:8080/api/articles/draft` 能透传 header

**交付物**：可路由的 `gateway-service`，外部请求统一走 8080。

---

### 阶段 4：网关动态限流（Day 6~7）⏱ ~1.5 天 ⭐ 高分重点

> 目标：`GET /api/articles/{id}` 同一 IP 10 秒内最多 20 次，可通过管理员接口动态调整。

#### 4.1 限流算法选型
采用 **Redis 固定窗口计数器**（最简方案，足够本地原型展示）：

- Key：`rate_limit:{ip}:{path}`（TTL = windowSeconds）
- 每次请求 `INCR`，超过 `maxRequests` 直接拒绝

也可以升级为 **滑动窗口**（Redis ZSET），但固定窗口代码量小，文档好写。

#### 4.2 限流配置数据源
按 `docs/coding-standards.md` 第 53~55 行 + `docs/database-design.md` 第 121~145 行：

- Redis Hash：`rate_limit_config:article_detail`
- 字段：`windowSeconds=10`, `maxRequests=20`, `enabled=true`
- 启动时从 Redis 读取（如果不存在，写入默认配置）

**新增** `RateLimitConfigService`：
```java
RateLimitConfig getConfig(String path);   // 从 Redis Hash 读
void updateConfig(String path, RateLimitConfig config);  // 写回 Redis
```

#### 4.3 限流 GlobalFilter
1. `filter/RateLimitGlobalFilter.java`：
   - 只对 `GET /api/articles/{id}` 生效（用 `PathMatcher`）
   - 拿客户端 IP（`X-Forwarded-For` 或 `RemoteAddress`）
   - 从 `RateLimitConfigService` 读当前配置
   - 走 Redis Lua 脚本原子执行 INCR + EXPIRE（避免竞态）
   - 超过 `maxRequests` 返回 429 + `ApiResponse.fail(429, "访问过于频繁，请稍后再试")`
2. 用 Lua 脚本保证原子性（避免 SETNX + EXPIRE 之间的 race condition）

#### 4.4 管理员修改限流配置接口
- `PUT /api/admin/rate-limit/article-detail`（由 user-service 暴露也可，由 gateway 暴露也可）
- 接收 `RateLimitConfigDTO { windowSeconds, maxRequests, enabled }`
- 写入 Redis Hash
- 立即生效（无需重启，限流 filter 每次从 Redis 读）

**推荐**：在 `user-service` 暴露这个接口（因为它已经有 user role 校验），不另起服务。

#### 4.5 验证
- 用 `ab -n 30 -c 1 "http://localhost:8080/api/articles/1"` 模拟 30 次请求
- 前 20 次返回 200，后 10 次返回 429
- `curl -X PUT -H "Authorization: Bearer <admin-token>" -d '{"windowSeconds":10,"maxRequests":50,"enabled":true}' .../api/admin/rate-limit/article-detail`
- 再 ab 测试 50 次都通过

**交付物**：可演示的限流效果 + 动态配置 + 报告中有"为什么使用网关限流"小节（9.1 高可用设计相关）。

---

### 阶段 5：补充联调 / 测试 / Swagger 聚合（Day 8）⏱ ~0.5 天

#### 5.1 Swagger 聚合
- 在 `gateway-service` 启动时聚合所有下游 `/v3/api-docs`
- 通过 `springdoc.swagger-ui.urls` 配 4 个服务
- 用户访问 `http://localhost:8080/doc.html` 看到所有服务的接口

#### 5.2 Postman 验证
- 把 C 的 Postman collection 跑通：注册 → 登录 → 创建草稿 → 发布 → 详情 → 点赞 → 评论 → Top10 → AI 续写 → 限流测试
- 把验证结果截图保存到 `docs/A/测试截图/`

#### 5.3 单元测试
- 给 `JwtUtil` 写单测（生成 + 解析 + 过期）
- 给 `RateLimitConfigService` 写单测（Redis 读 / 写 / 默认值兜底）

**交付物**：所有接口在 Postman 中可跑通；Swagger 文档聚合可用。

---

### 阶段 6：报告章节撰写（Day 8~9）⏱ ~1 天

> A 负责的报告章节（来自 PDF 第 23~24 页）：

| 章节 | 标题 | 内容要点 | 预计字数 |
| --- | --- | --- | --- |
| 1 | 项目概述 | 项目背景、目标、技术栈选型理由 | 800 |
| 2 | 系统总体设计 | 5 服务架构图、技术选型表、部署架构 | 1500 |
| 5.1 | 用户认证与 JWT 鉴权 | JWT 原理、为什么选无状态、Token 黑名单、生产建议 | 1200 |
| 5.4 | 网关动态限流功能 | 为什么用网关、限流算法、Redis 配置、动态生效截图 | 1500 |
| 8.1 | 为什么使用微服务架构 | 单体 vs 微服务、本项目为什么拆 5 个、粒度讨论 | 1000 |
| 8.4 | 为什么使用网关限流 | 网关限流 vs 服务限流、动态配置 vs 静态配置 | 800 |
| 9.1 | 高可用设计 | 网关多实例、Redis 主从、限流降级、Token 黑名单、链路追踪 | 1200 |

每章给 **1 张架构图（draw.io 截图）** + **1 段关键代码截图** + **1 个生产环境思考**。

**报告统一格式要求**（与 B、C 对齐）：
- 中文章节用三级标题 `## 5.1 用户认证与 JWT 鉴权`
- 代码块用 ` ```java `
- 架构图用 mermaid 或 draw.io 截图
- 末尾附 **生产环境思考** 一节，给老师留加分项

---

## 四、关键技术决策与风险点

### 4.1 技术决策记录

| 决策 | 选择 | 理由 |
| --- | --- | --- |
| 注册中心 | **暂不引入 Nacos** | 本地原型，配置文件硬编码下游地址即可；报告里讲生产环境用 Nacos 即可 |
| 鉴权方式 | **JWT 无状态** | 符合微服务理念，网关校验后透传 userId |
| 限流算法 | **Redis 固定窗口 + Lua** | 代码量小、原子性有保证、文档好写 |
| 限流配置存储 | **Redis Hash** | 数据库表是可选方案；Hash 写起来更轻 |
| Token 黑名单 | **Redis SET + TTL** | 简单；登出时写入，校验时检查 |
| 数据库 | MySQL（生产）+ H2（本地） | 复用 B 的 DatabaseInitializer 模式 |
| API 文档 | **Knife4j** | 中文友好、UI 美观 |
| 异步通信 | **HTTP 调用**（跨服务） + **MQ**（异步任务） | 同步业务走 HTTP，耗时任务走 MQ |

### 4.2 风险点与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| B 的 Controller 没走 `ApiResponse` | 联调时各服务返回结构不一致 | 写一份《B 模块整改清单》发给 B，并要求 PR 合并前由 A review |
| `ranking-service` 缺少文章信息补全 | Top10 只返回 ID | 让 B 加 FeignClient 调用 article-service |
| 网关限流是高频考点，老师会重点问 | 限流失效会被扣分 | 写 1 个 demo 脚本（ab / wrk）+ 截图 |
| 报告要写 7 章，A 工作量大 | 时间紧 | 提前 1 天开始写，每章 800~1500 字，模板化产出 |
| A 也要在 Postman 测试 | 浪费时间 | 复用 C 的 Postman collection，只跑与 A 相关的 5 个接口 |

### 4.3 给 B / C 的协作请求（建议今天同步出去）

**致 B**：
1. 你的 `article-service` 所有 Controller 必须改用 `ApiResponse<T>` 返回，否则与 `docs/coding-standards.md` 第 12~21 行不一致
2. 删除每个 Controller 里的 `try-catch (RuntimeException)`，统一抛 `BusinessException`
3. `Article` 实体请 `extends BaseEntity`，不要重复声明 `id/createTime/...`
4. `ranking-service` 的 Top10 返回要补全文章标题/作者/摘要
5. 把 article-service 的 `RankingService` 删掉，热度写入统一收口到 `ranking-service`

**致 C**：
1. `ai-service` 启动后请告诉我，我要在 `gateway-service` 加路由
2. SSE 接口的 IP 限流我已经做了，不需要重复做
3. Postman collection 请保持和 `docs/api-spec.md` 一致

---

## 五、验收标准（A 部分的"必须完成"清单）

按 PDF 第 30 页"第一优先级：必须完成"，A 负责交付的底线：

- [x] **项目整体骨架搭建**（根 pom / common / docker-compose） ✅ 已完成
- [ ] **user-service 能启动**，4 个接口（注册/登录/注销/资料）能调通
- [ ] **gateway-service 能启动**，5 个服务路由全通
- [ ] **JWT 鉴权** 跑通，登录后能拿到 token 并访问需登录接口
- [ ] **网关动态限流** 跑通，10s/20次能演示，管理员接口能改配置
- [ ] **Swagger/OpenAPI 文档** 至少 user / article / ranking / ai 4 个服务可见
- [ ] **报告 A 负责章节**（1 / 2 / 5.1 / 5.4 / 8.1 / 8.4 / 9.1）全部完成

---

## 六、时间表（按 9 天倒推）

| Day | 主要工作 | 交付物 |
| --- | --- | --- |
| Day 1 | 阶段 1：项目骨架 + common 增强 | 根 pom 完整、common 工具类、docker-compose 完善 |
| Day 2~3 | 阶段 2：user-service | 4 个接口跑通 + Postman 自测 |
| Day 4~5 | 阶段 3：gateway-service | 5 服务路由打通 + JWT 鉴权 + 角色透传 |
| Day 6~7 | 阶段 4：动态限流 ⭐ | 限流 + 管理员配置 + 演示脚本 |
| Day 8 | 阶段 5：联调 + Swagger | Postman 跑通 + Swagger 聚合 |
| Day 9 | 阶段 6：报告章节 | 7 章完成 + 架构图 + 截图 |
| Day 10 | 缓冲 / 修改 / 提交 | 全员 review + 演示视频 |

---

## 七、参考资料

- [x] `docs/api-spec.md` — 接口规范（路径、返回、错误码、JWT Header）
- [x] `docs/architecture.md` — 系统架构（5 服务、链路、Redis Key、MQ 结构）
- [x] `docs/coding-standards.md` — 代码规范（统一返回、统一异常、Redis Key、MQ 结构）
- [x] `docs/database-design.md` — 数据库表设计 + Redis Key 设计
- [x] `docs/deployment-notes.md` — 本地与生产部署说明
- [x] `docs/task-list.md` — 任务清单
- [x] `docs/database-init.sql` — MySQL 初始化脚本
- [x] 《项目分析以及分工.pdf》第 22~24 页（成员 A 详细任务）

---

**总结**：A 负责的 gateway + user 是整个系统的"门面"和"骨架"，必须先于 B / C 跑通。阶段 4 的"动态限流"是老师最可能现场问的点（建议熟读 Redis Lua 脚本和滑动窗口的演进方案），要多花时间打磨并写进报告。**先把骨架立起来，再去推动 B 整改，最后写报告拿满分。**
