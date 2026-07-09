# AI-KnowledgeHub 最终验收视频分镜脚本

本文档用于课程设计最终录屏。所有命令默认在项目根目录执行：

```powershell
cd "D:\大三下作业\web后端\ai-knowledgehub"
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
```

录屏核心原则：

- 所有外部接口统一通过 `http://localhost:8080` Gateway 访问。
- MySQL、Redis、RabbitMQ、Nacos 必须使用真实中间件。
- 分镜中的 `[PASS]`、`UP`、`zset`、`429`、`data:` 等输出就是验收证据。

---

## 准备阶段：启动环境

### 命令

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-all-services.ps1

docker compose up -d

docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

netstat -ano | Select-String ":6379.*LISTENING"

docker exec akh-redis redis-cli DEL article:hot:ranking

powershell -ExecutionPolicy Bypass -File scripts\start-all-services.ps1 -RankingUseRedis
```

确认全服务：

```powershell
netstat -ano | Select-String ":808[0-4].*LISTENING"

curl.exe http://localhost:8080/actuator/health
```

### 输出怎么看

- `docker ps` 中应看到 `akh-mysql`、`akh-redis`、`akh-rabbitmq`、`akh-nacos` 正在运行。
- `netstat :6379` 应只保留 Docker 相关监听，避免本机 Redis 抢占端口。
- `start-all-services.ps1 -RankingUseRedis` 会启动 5 个 Java 微服务，并强制 `ranking-service` 使用 Redis ZSET。
- `netstat :808[0-4]` 应看到 `8080` 到 `8084` 全部 `LISTENING`。
- `curl /actuator/health` 应返回：

```json
{"status":"UP"}
```

### 对应验收点

- 工具证明：真实 MySQL、Redis、RabbitMQ、Nacos、Gateway。
- 工程化可复现：Docker Compose + PowerShell 脚本启动。
- 架构要求：5 个微服务 + 统一网关入口。

---

## 分镜 1：启动总览

### 命令

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

netstat -ano | Select-String ":808[0-4].*LISTENING"

curl.exe http://localhost:8080/actuator/health
```

### 讲述稿

这一段展示系统完整运行环境。Docker 中运行 MySQL、Redis、RabbitMQ、Nacos 四类真实中间件，Java 侧运行 user、article、ranking、ai、gateway 五个微服务。最终入口是 `8080` 的 gateway-service，其他微服务只作为内部服务被网关转发。

### 输出怎么看

- `akh-mysql`：关系型数据库，用于用户、文章、评论、点赞等持久化。
- `akh-redis`：用于热榜 ZSET、网关限流计数、Token 黑名单。
- `akh-rabbitmq`：用于文章发布后的 Fanout 异步广播。
- `akh-nacos`：用于动态下发网关限流配置。
- `8080`：Gateway 统一入口。
- `8081`：User Service。
- `8082`：Article Service。
- `8083`：Ranking Service。
- `8084`：AI Service。
- `{"status":"UP"}`：Gateway 和关键依赖均正常。

### 对应验收点

- 工具证明：真实关系型数据库、Redis、MQ、网关等中间件。
- 系统总体设计：微服务拆分与统一入口。
- 配置与部署：Docker Compose 和脚本化启动。

---

## 分镜 2：用户权限与 JWT

### 命令

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1
```

### 讲述稿

这一段验证用户权限体系。脚本会通过网关完成注册、登录，登录成功后获得 JWT。随后验证未登录访问受保护接口返回 401，携带 Token 可以访问 profile，并且即使客户端伪造 `X-User-Id`、`X-User-Role`，网关也会覆盖这些请求头，只信任 JWT 解析结果。最后验证注销后旧 Token 失效，以及普通用户访问管理员接口返回 403。

### 重点输出

```text
[PASS] gateway health -> HTTP 200
[PASS] register -> HTTP 200
[PASS] login -> HTTP 200
[PASS] profile without token -> HTTP 401
[PASS] profile with token and forged headers -> HTTP 200
[PASS] logout -> HTTP 200
[PASS] profile with logged-out token -> HTTP 401
[PASS] normal user calls admin API -> HTTP 403
```

### 输出怎么看

- `register/login HTTP 200`：用户注册登录链路正常。
- `profile without token -> 401`：受保护接口需要认证。
- `profile with token and forged headers -> 200`：网关解析 JWT 并覆盖伪造用户头。
- `logged-out token -> 401`：注销后 Token 进入 Redis 黑名单，旧 Token 不再可用。
- `admin API -> 403`：普通用户不能访问管理员接口。

### 对应验收点

- 基础支撑功能：注册、登录、注销。
- 用户权限：普通用户和管理员角色区分。
- JWT：Token 颁发、校验、失效。
- Gateway：统一鉴权、可信用户信息透传。

---

## 分镜 3：文章 CRUD、评论点赞、RabbitMQ

### 命令

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1
```

可选打开 RabbitMQ 控制台：

```powershell
start http://localhost:15672
```

账号：

```text
guest / guest
```

### 讲述稿

这一段继续使用主验收脚本，重点展示文章业务链路。脚本会创建文章草稿、发布文章、分页获取最新文章、修改文章、获取文章详情、逻辑删除文章，并验证删除后的文章详情不再作为成功结果返回。同时还会验证评论入库、点赞入库，以及文章发布后 RabbitMQ Fanout 队列收到处理证据。

### 重点输出

```text
[PASS] create article draft -> HTTP 200
[PASS] publish article -> HTTP 200
[PASS] latest articles -> HTTP 200
[PASS] update article -> HTTP 200
[PASS] article detail -> HTTP 200
[PASS] article detail contains updated title
[PASS] logical delete article -> HTTP 200
[PASS] deleted article detail -> no success response
[PASS] add comment -> HTTP 200
[PASS] like article -> HTTP 200
[PASS] RabbitMQ queue article.tag.queue
[PASS] RabbitMQ queue article.audit.queue
```

### 输出怎么看

- `create draft`：文章草稿创建成功。
- `publish article`：文章从草稿变为发布状态。
- `latest articles`：支持分页获取最新文章。
- `update article` 和 `contains updated title`：修改后的标题能在详情中体现。
- `logical delete`：删除接口成功执行逻辑删除。
- `deleted article detail -> no success response`：已删除文章不会作为正常详情返回。
- `add comment`：评论写入数据库。
- `like article`：点赞写入数据库并更新统计。
- RabbitMQ 两个队列：文章发布事件通过 Fanout 广播到独立消费者队列。

### 对应验收点

- 基础支撑功能：文章创建、发布、修改、逻辑删除、分页列表、详情查询。
- 互动基础功能：评论入库、单次点赞更新数据库。
- 核心架构功能：长文发布后投递 RabbitMQ，Fanout 广播给多个下游处理逻辑。
- 工具证明：RabbitMQ 使用真实队列。

---

## 分镜 4：Redis ZSET 热榜

### 命令

```powershell
docker exec akh-redis redis-cli DEL article:hot:ranking

powershell -ExecutionPolicy Bypass -File scripts\acceptance-redis-ranking.ps1
```

可选手动展示 Redis 原始数据：

```powershell
docker exec akh-redis redis-cli TYPE article:hot:ranking

docker exec akh-redis redis-cli ZCARD article:hot:ranking

docker exec akh-redis redis-cli ZREVRANGE article:hot:ranking 0 9 WITHSCORES
```

### 讲述稿

这一段验证实时热榜。脚本先清空本轮演示用的热榜 key，然后通过 Gateway 调用 ranking-service 的加分接口，模拟文章发布、阅读、点赞、评论带来的热度变化。随后脚本分别读取 HTTP Top10 接口和 Docker Redis 容器中的 `article:hot:ranking` ZSET，逐项比较文章 ID 和热度分数。

### 重点输出

```text
[PASS] ranking demo user logged in through gateway
[PASS] ranking demo scores submitted through gateway
TYPE article:hot:ranking = zset
ZCARD article:hot:ranking = 2
ZREVRANGE article:hot:ranking 0 9 WITHSCORES:
9101
8
9102
5
HTTP data.total = 2   Redis ZCARD = 2   Redis top count = 2
[PASS] HTTP Top10 matches Redis ZSET article:hot:ranking
```

### 输出怎么看

- `TYPE = zset`：Redis key 类型确实是 Sorted Set。
- `ZCARD = 2`：当前热榜里有 2 篇演示文章。
- `9101 8`：文章 9101 的热度分数为 8，来自 publish + view + like。
- `9102 5`：文章 9102 的热度分数为 5，来自 publish + comment。
- `HTTP Top10 matches Redis ZSET`：HTTP 接口返回的数据与 Redis 内部 ZSET 完全一致。

### 对应验收点

- 核心架构功能：文章实时热搜榜。
- Redis 技术点：使用 ZSET 维护热度分数和 Top10 排名。
- 工具证明：直接进入 Docker Redis 容器读取真实 ZSET 数据。

---

## 分镜 5：Nacos 动态限流

### 命令

```powershell
$adminBody = '{"username":"admin_demo","password":"123456"}'

$adminResp = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/user/login" -ContentType "application/json" -Body $adminBody

$adminToken = $adminResp.data.token

Write-Host ("admin token length = " + $adminToken.Length)

powershell -ExecutionPolicy Bypass -File scripts\acceptance-nacos-rate-limit.ps1 -AdminToken $adminToken
```

可选打开 Nacos 控制台：

```powershell
start http://localhost:8848/nacos
```

控制台中查看：

```text
dataId: gateway-service.yml
group: DEFAULT_GROUP
```

### 讲述稿

这一段验证动态网关限流。首先使用内置管理员账号 `admin_demo` 登录，获得管理员 Token。脚本随后创建一篇用于限流测试的文章，并通过管理员接口把文章详情限流配置发布到 Nacos，把 `maxRequests` 临时改为 5。脚本不会立刻只读一次 Nacos，而是最多等待 12 秒轮询，避免 Nacos 短暂延迟造成误判。配置下发后，Gateway 无需重启即可刷新配置。随后脚本连续访问文章详情，第 6 次左右触发 `429 Too Many Requests`，最后恢复默认限流配置 `maxRequests=20`。

### 重点输出

```text
admin token length = 339
[PASS] admin publishes Nacos rate limit to 5 -> HTTP 200
waiting Nacos config 1/12 ...
[PASS] Nacos config contains max-requests: 5
[PASS] gateway refreshed Nacos config: "maxRequests":5
rate-limit request 1 -> HTTP 200
rate-limit request 2 -> HTTP 200
rate-limit request 3 -> HTTP 200
rate-limit request 4 -> HTTP 200
rate-limit request 5 -> HTTP 200
rate-limit request 6 -> HTTP 429
[PASS] Nacos dynamic rate limit returned 429
[PASS] restore default Nacos rate limit -> HTTP 200
[PASS] gateway refreshed Nacos config: "maxRequests":20
```

### 输出怎么看

- `admin token length`：管理员登录成功并获得 JWT。
- `admin publishes Nacos rate limit to 5`：管理员接口成功发布限流配置。
- `waiting Nacos config`：脚本正在等待 Nacos 配置中心完成写入/读取同步，这是正常现象。
- `Nacos config contains max-requests: 5`：Nacos 中的 `gateway-service.yml` 已经变为新限流值。
- `gateway refreshed ... "maxRequests":5`：Gateway 已经热刷新到新配置，无需重启。
- `HTTP 429`：限流生效，超过阈值的文章详情请求被网关拒绝。
- `restore default ... maxRequests:20`：演示后恢复默认限流配置，避免影响后续测试。

### 对应验收点

- 核心架构功能：动态网关限流。
- 配置中心要求：限流参数由 Nacos 动态下发，而不是写死在代码里。
- 管理员权限：只有 ADMIN Token 可以修改限流配置。
- Gateway 能力：无需重启即可刷新限流配置。
- Redis 能力：Redis 只用于限流计数，Nacos 负责配置源。

---

## 分镜 6：AI 续写与 SSE 流式输出

### 命令：注册并登录 AI 演示用户

```powershell
$ts = Get-Date -Format "HHmmss"
$uname = "demo_ai_$ts"
$aiBody = '{"username":"' + $uname + '","password":"123456"}'

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/user/register" -ContentType "application/json" -Body $aiBody | Out-Null

$loginResp = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/user/login" -ContentType "application/json" -Body $aiBody

$tok = $loginResp.data.token
```

### 命令：普通续写

```powershell
$sync = curl.exe -s -X POST -H "Authorization: Bearer $tok" -H "Content-Type: application/json" -d '{"prompt":"请写一段 Redis ZSET 的优势"}' "http://localhost:8080/api/ai/continue-writing"

$sync | ConvertFrom-Json | ConvertTo-Json -Depth 5
```

### 命令：SSE 流式续写

```powershell
Add-Type -AssemblyName System.Web

$promptUtf8 = [System.Web.HttpUtility]::UrlEncode("请写一段 Redis ZSET 的优势")

curl.exe -sN -H ("Authorization: Bearer " + $tok) ("http://localhost:8080/api/ai/continue-writing/stream?prompt=" + $promptUtf8)
```

### 讲述稿

这一段验证 AI 创作辅助。普通续写接口会一次性返回完整文本，适合普通 API 调用；SSE 流式续写接口会持续输出多帧 `data:`，模拟 AI 生成内容逐步推送给客户端。所有请求仍然带 JWT 并通过 Gateway 访问。

### 重点输出

```text
{
  "code": 200,
  "data": ...
}

data: ...
data: ...
data: ...
```

### 输出怎么看

- 普通续写返回 `code=200`：AI 同步续写接口正常。
- SSE 输出多行 `data:`：服务端通过 Server-Sent Events 流式推送内容。
- `curl -sN`：禁用缓冲，便于看到流式帧持续输出。

### 对应验收点

- 核心架构功能：AI 流式创作辅助。
- SSE 技术点：Server-Sent Events 持续推送。
- Gateway 和 JWT：AI 接口同样经过统一入口和鉴权。

---

## 分镜 7：收尾

### 命令

```powershell
Get-Content docs\acceptance\runtime\last-gateway-acceptance.json -Tail 80

powershell -ExecutionPolicy Bypass -File scripts\stop-all-services.ps1
```

如果最终要关闭中间件：

```powershell
docker compose down
```

### 讲述稿

最后展示验收脚本的运行记录，并停止 Java 微服务。中间件可以保留，方便老师或助教继续复查；如果需要完全释放环境，再执行 `docker compose down`。

### 输出怎么看

- `last-gateway-acceptance.json`：记录最近一次网关验收的接口结果。
- `stop-all-services.ps1`：停止 5 个 Java 微服务。
- `docker compose down`：关闭 MySQL、Redis、RabbitMQ、Nacos 等中间件。

### 对应验收点

- 工程化验收：脚本有记录、可复现。
- 提交材料：验收结果可写入报告或作为截图依据。

---

## 附：API 文档与 Postman 验收材料

### Swagger / OpenAPI

启动服务后访问：

```text
http://localhost:8081/doc.html
http://localhost:8082/doc.html
http://localhost:8083/doc.html
http://localhost:8084/doc.html
```

OpenAPI JSON：

```text
http://localhost:8081/v3/api-docs
http://localhost:8082/v3/api-docs
http://localhost:8083/v3/api-docs
http://localhost:8084/v3/api-docs
```

### Postman Collection

推荐提交和导入：

```text
postman/AI-KnowledgeHub-Gateway-Acceptance.postman_collection.json
```

说明：

- 全部请求默认通过 `http://localhost:8080` Gateway。
- 集合内包含健康检查、注册登录、JWT、文章、评论点赞、热榜、AI、管理员限流等核心链路。
- 导入 Postman 后按顺序运行即可验证核心业务流程。


