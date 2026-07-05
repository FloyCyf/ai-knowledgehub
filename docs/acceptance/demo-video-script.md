# AI-KnowledgeHub  5-Minute Demo Video Walkthrough

> **任务编号**:题目三《基于 AI 的"智能知识库与内容发布"平台》
> **总预算**:5 分钟以内(实际建议录制 4 分 40 秒,留 20 秒弹性)
> **核心交付物**:录屏 mp4,对应指导书 Page 3-4 §3.2 之要求:"展示各个微服务和中间件的启动状态" + "演示主要业务链路的流转"
> **录制建议工具**:OBS Studio 1080p30 / Windows Terminal + PowerShell

---

## 0. 总览时间线

| 时间码 | 分镜 | 主题 | 工具形态 |
|---|---|---|---|
| 0:00 - 0:25 | 1 | 启动总览:5 微服务 + 3 中间件 | 手动 |
| 0:25 - 1:15 | 2 | 网关统一入口 + JWT 鉴权穿透 + admin 403 | `acceptance-gateway.ps1` 段 1 |
| 1:15 - 1:55 | 3 | 文章发布 + MQ Fanout + AI 分析 + 涨分 | `acceptance-gateway.ps1` 段 2 + 手动补涨分 |
| 1:55 - 2:25 | 4 | Redis ZSET 直击证据(数据真实持久化) | 手动 `redis-cli` + 跨校验 |
| 2:25 - 3:10 | 5 | 网关动态限流触发 429 | `acceptance-gateway.ps1 -AdminToken` |
| 3:10 - 3:55 | 6 | AI 续写(同步 + SSE 流式) | 手动 `curl` |
| 3:55 - 4:10 | 7 | 收尾:关停服务 + 架构亮点口播 | 手动 |

> 本脚本采用"自动化脚本触发 + 关键证据手动复现"的混合格式。自动化保证链接全打通、不漏步;手动保证最直观的证据(Redis ZSET、429、逐字 SSE)能被老师看清楚。
>
> **录屏工程说明**:`acceptance-gateway.ps1` 一旦启动会顺次跑完所有 9 段(line 127-254),期间没有 stop-here 钩子。本脚本固定采用 **OBS 双 PowerShell 窗口并排录制**:左窗口跑脚本,右窗口做手动操作;两个窗口并行执行,OBS 一镜全收,左右画面同步呈现给老师。详见 `§3.8`。

---

## 1. 前置准备(录屏前 5 分钟)

```powershell
# (a) 切到 UTF-8 控制台,避免 PowerShell 中文乱码
chcp 65001

# (b) 启 Docker 三个中间件
docker compose up -d
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# (c) 启 5 个 Java 服务(脚本会自动 mvn -DskipTests install 然后按
#     user -> article -> ranking -> ai -> gateway 顺序拉起)
powershell -ExecutionPolicy Bypass -File scripts\start-all-services.ps1

# (d) 等服务起来后,确认五口在听
netstat -ano | Select-String ":808[0-4].*LISTENING"
curl.exe http://localhost:8080/actuator/health
```

**期望**:`docker ps` 三行均 `Up ... (healthy)`,5 个端口全 LISTENING,`/actuator/health` 返回 `{"status":"UP"}`。

---

## 2. 分镜逐稿

### 分镜 1 / 启动总览(0:00 - 0:25)

**目的**:满足指导书"展示各个微服务和中间件的启动状态"。

**手动命令**(每条 3 秒,留 1-2 秒看输出):

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

```powershell
netstat -ano | Select-String ":808[0-4].*LISTENING"
```

```powershell
curl.exe http://localhost:8080/actuator/health
```

**截图点 S1**:

- S1-1:`akh-mysql / akh-redis / akh-rabbitmq` 三行 healthy
- S1-2:5 个端口 LISTENING
- S1-3:网关 UP

**口播稿**:**"系统为 5 个 Spring Boot 微服务(端口 8080-8084)+ 3 个中间件。MySQL 持久化,Redis ZSET 热榜 + 网关限流,RabbitMQ Fanout 异步广播,SSE 流式 AI。所有外部请求统一经 gateway-service 进入。"**

---

### 分镜 2 / 网关鉴权穿透(0:25 - 1:15)

**目的**:印证"所有外部请求经 gateway-service 统一鉴权;JWT 解析覆盖伪造 X-User-* 头;普通用户访问 admin 接口 403"。

**操作 — 直接跑 acceptance-gateway.ps1 不带 -AdminToken**:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1
```

**脚本会跑到 line 254(无 -AdminToken,Rate limit check [SKIP])自动结束**。录像里只需要**关注前三段对应的 6 个 [PASS]**:

```
==> Gateway health
[PASS] gateway health -> HTTP 200

==> User register and login through gateway
[PASS] register -> HTTP 200
[PASS] login -> HTTP 200

==> JWT protection and trusted gateway headers
[PASS] profile without token -> HTTP 401
[PASS] profile with token and forged headers -> HTTP 200

==> Admin permission check with normal user token
[PASS] normal user calls admin API -> HTTP 403
```

**截图点 S2**:

- S2-1:终端上 6 个 `[PASS]` 行(连成一片)
- S2-2:同时显示 `Username` 自动注册成 `a_accept_<时间戳>`(临时用户)
- S2-3:脚本运行结束后落地 JSON `docs/acceptance/runtime/last-gateway-acceptance.json`

**口播稿**:**"我们让脚本以一个新注册用户走网关:`gateway health 200`、注册和登录 200、profile 不带 token 返回 401。**带 token 但请求头里塞了伪造的 X-User-Id=999999 时,网关的 `JwtAuthenticationFilter` 解析 JWT 后,会以解析结果为准,所以 profile 仍然 200,但响应里的 id 是这个**真实用户**的 id,不是 999999。最后,普通用户去 PUT `/api/admin/rate-limit/article-detail` 返回 403,这是 `AdminAuthorizationWebFilter` 在起作用 — 它专门守护挂在网关本地的管理员接口,因为这类接口不走 Spring Cloud Gateway 的 GlobalFilter。"**

---

### 分镜 3 / 文章发布 + MQ + AI(1:15 - 1:55)

**目的**:题目三核心架构点 — "文章发布 → MQ Fanout → 多下游 AI 处理"。

**⚠ 双窗口并排**:本分镜的"前半"在 OBS 左窗口(跑 acceptance),"后半"在 OBS 右窗口(手动涨分),两个窗口并行执行。

#### 左窗口(录像中持续可见的脚本终端)

`acceptance-gateway.ps1` 继续自动跑(line 172-214):

```
==> Article route through gateway
[PASS] create article draft -> HTTP 200
[PASS] publish article -> HTTP 200
[PASS] latest articles -> HTTP 200
[PASS] article detail -> HTTP 200

==> Ranking and AI routes through gateway
[PASS] ranking top10 -> HTTP 200
[PASS] ai continue writing -> HTTP 200
[PASS] ai sse stream -> HTTP 200
[PASS] ai article analysis -> HTTP 200
```

> 录取时这一段约 6-8 秒。Acceptance 会在第 13 个 [PASS] 后继续跑 Rate limit check([SKIP]),可直接 Ctrl+C 中止,JSON 已落地。

#### 右窗口(同时进行的手动操作)

```powershell
chcp 65001

# (1) 自取一个新用户,不依赖左窗口的 session
#     PS 5.1:单引号拼接字符串,不要 ConvertTo-Json 也不要反引号转义
$ts   = Get-Date -Format "HHmmss"
$uname = "demo_bump_$ts"
$body = '{"username":"' + $uname + '","password":"123456"}'
curl.exe -s -X POST -H "Content-Type: application/json" -d $body `
   "http://localhost:8080/api/user/register" | Out-Null
$loginResp = curl.exe -s -X POST -H "Content-Type: application/json" -d $body `
   "http://localhost:8080/api/user/login"
$tok = ($loginResp | ConvertFrom-Json).data.token
Write-Host ("bump token length = " + $tok.Length)

# (2) 自创建 3 篇短文并发布,这样 Redis ZSET 自动得到 +2 热度
1..3 | ForEach-Object {
    $draft = curl.exe -s -X POST -H "Authorization: Bearer $tok" -H "Content-Type: application/json" `
       -d '{"title":"ranking demo article","content":"ranking demo content"}' `
       "http://localhost:8080/api/articles/draft"
    $aid = ($draft | ConvertFrom-Json).data.articleId
    $null = curl.exe -s -X POST -H "Authorization: Bearer $tok" `
       ("http://localhost:8080/api/articles/" + $aid + "/publish")
    Write-Host ("article " + $aid + " published")
}

# (3) 给第一篇点 2 个赞,加 1 条评论,加 1 次浏览
$first = 1   # 接受"最近创建的是某个 ID"的事实,直接用 $latest
$latest = curl.exe -s "http://localhost:8080/api/articles/latest"
$aid = ($latest | ConvertFrom-Json).data.list[0].id
1..2 | ForEach-Object {
    $null = curl.exe -s -X POST -H "Authorization: Bearer $tok" `
       ("http://localhost:8080/api/articles/" + $aid + "/like")
}
$null = curl.exe -s -X POST -H "Authorization: Bearer $tok" -H "Content-Type: application/json" `
   -d '{"content":"good write-up"}' `
   ("http://localhost:8080/api/articles/" + $aid + "/comments")
$null = curl.exe -s -X POST -H "Authorization: Bearer $tok" `
   ("http://localhost:8080/api/articles/" + $aid + "/view")
Write-Host "score bumped"
```

**截图点 S3**:

- S3-1:终端 8 个 `[PASS]` 后半段(line 172-214)
- S3-2:涨分 3 行写入后(可读 `docs/acceptance/runtime/logs/ranking-service.out.log` 看到 `文章 N 热度更新(Redis)` 三条)
- S3-3:同时打开 `http://localhost:15672`(RabbitMQ Management,Guest/Guest),能看到 `article.tag.queue` 与 `article.audit.queue` 各收到一条消息(Visual evidence)
- S3-4:`/api/ai/articles/{id}/analysis` 返回的 JSON,`data.tags` 与 `data.audit` 都有内容

**口播稿**:**"现在这篇文章被发布,脚本里我们看到 article-service 把状态切到 PUBLISHED、ranking 同步 +2 热度、并向 RabbitMQ 投递了一条消息。接下来我在 RabbitMQ Management 后台 — `article.tag.queue` 是 AI 标签消费者在监听,`article.audit.queue` 是 AI 合规检测消费者在监听。两条队列独立处理同一篇文章。最后 `ai-service` 把标签和合规检测结果写回 `article_ai_tag` 和 `article_audit_result` 表,我们查询 `/api/ai/articles/{id}/analysis` 拿到这两份结果。"**

---

### 分镜 4 / Redis ZSET 直击证据(1:55 - 2:25)

**目的**:这是题目三最直观的 Redis 证据,让 ZSET 看得到摸得着。

**操作 — 全部手动**:

```powershell
# (1) HTTP 接口读 top10
$top = curl.exe -s "http://localhost:8080/api/ranking/top10"
$topObj  = $top | ConvertFrom-Json
$top | ConvertFrom-Json | ConvertTo-Json -Depth 5

# (2) redis-cli 直读 ZSET(三条命令分开执行,演示更清楚)
Write-Host "---- TYPE ----"
redis-cli -h localhost TYPE  article:hot:ranking
Write-Host "---- ZCARD ----"
redis-cli -h localhost ZCARD article:hot:ranking
Write-Host "---- ZREVRANGE 0 9 WITHSCORES ----"
redis-cli -h localhost ZREVRANGE article:hot:ranking 0 9 WITHSCORES

# (3) 跨校验:HTTP data.total == Redis ZCARD
$card = (redis-cli -h localhost ZCARD article:hot:ranking) -as [int]
Write-Host ("")
Write-Host ("HTTP data.total = " + $topObj.data.total + "   Redis ZCARD = " + $card)
if ($topObj.data.total -eq $card) { "MATCH (Redis ZSET 是热榜唯一数据源)" } else { "DIFF" }
```

**截图点 S4**:

- S4-1:HTTP 接口 data.total=N,articles 按 hotScore 倒序
- S4-2:`TYPE` 返回 `zset`
- S4-3:`ZREVRANGE 0 9 WITHSCORES` 输出 — 真实在 Redis 里的 ZSET 持久化数据
- S4-4:终端最后输出 `MATCH (Redis ZSET 是热榜唯一数据源)`

**口播稿**:**"现在打开 redis-cli 直接打 `article:hot:ranking` 这个 key:`TYPE` 是 `zset`,`ZCARD` 等于 N,`ZREVRANGE` 把所有 articleId 和分数倒序列出来 — 这是 Redis 内部的真实持久化数据。HTTP 接口的 `data.total` 与 `ZCARD` 完全相等,说明我们**没有写两套数据**,Redis ZSET 就是热榜的唯一数据源。每篇文章的 view +1、publish +2、comment +3、like +5 通过 `ZINCRBY` 写进来,Top10 通过 `ZREVRANGE 0 9 WITHSCORES` 拿出去,这就是 Redis ZSET 的标准用法。"**

> 文字证据同时落到 `docs/acceptance/ranking/b-redis-zset-evidence.txt`,本分镜与该 txt 一一对应。

### 3.1 双窗口 OBS 配置指引

```
+-------------------------------------------+-------------------------------------------+
| OBS 场景 1:PowerShell-A(左 50%)          | OBS 场景 1:PowerShell-B(右 50%)          |
| (跑 acceptance-gateway.ps1)               | (手动 redis-cli / curl)                   |
|                                           |                                           |
| chcp 65001                                | chcp 65001                                |
| powershell -ExecutionPolicy Bypass \      | $ts = Get-Date -Format "HHmmss"           |
|   -File scripts\acceptance-gateway.ps1    | $body = "{...}"                            |
|                                           | $tok = curl ...                          |
| ==> Gateway health                        |   ...                                     |
| [PASS] gateway health -> HTTP 200         |                                           |
| [PASS] register -> HTTP 200               |                                           |
| ...                                       |                                           |
| Acceptance finished.                      |                                           |
+-------------------------------------------+-------------------------------------------+
                 OBS 1080p30 一次性录完全部 5 分钟
```

**OBS 操作要点**:
- 新建"Display Capture"或"Window Capture" → 选 PowerShell 窗口
- 调整"Position + Size"把第一个窗口放左半屏、第二个窗口放右半屏
- 添加"Text (GDI+)"图层覆盖底部,展示当前分镜编号和口播要点
- 录制时按"开始录制" → 中间任何时刻都不要切换窗口或切桌面(OBS 中止)

**Window Capture 的兼容性**:PowerShell 5.1 默认窗口标题是 `Administrator: Windows PowerShell`,便于 OBS 锁定窗口。Windows Terminal 标题更短(推荐)。

---

### 分镜 5 / 网关动态限流 429(2:25 - 3:10)

**目的**:印证"网关层动态下发限流策略,无需重启服务"。

**步骤 A — 准备 ADMIN token**(分镜 5 开始前一次性准备):

```powershell
# (A1) 把历史账户 a_accept_20260705190800 提为 ADMIN
docker exec akh-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD:-} user_db -e "
  UPDATE user SET role='ADMIN' WHERE username='a_accept_20260705190800';
  SELECT id, username, role FROM user WHERE role='ADMIN';
" 2>&1 | Out-File "last-h2-promote.txt"

# (A2) 登录这个 admin 用户拿 admin token(用 curl.exe 避免 PS hashtable 坑)
$adminBody = '{"username":"a_accept_20260705190800","password":"123456"}'
$adminResp = curl.exe -s -X POST -H "Content-Type: application/json" -d $adminBody `
   "http://localhost:8080/api/user/login"
$adminToken = ($adminResp | ConvertFrom-Json).data.token
Write-Host ("admin token length = " + $adminToken.Length)
```

**步骤 B — 跑带 -AdminToken 的脚本**:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1 -AdminToken $adminToken
```

**这次脚本不会重新注册用户**(会发现 register 已存在或者 username 时间戳仍然新增),但关键是最后两段(限流相关)会跑:

```
==> Rate limit check
[PASS] admin updates rate limit to 5 -> HTTP 200
rate-limit request 1 -> HTTP 200
rate-limit request 2 -> HTTP 200
rate-limit request 3 -> HTTP 200
rate-limit request 4 -> HTTP 200
rate-limit request 5 -> HTTP 200
rate-limit request 6 -> HTTP 429
[PASS] rate limit returned 429
[PASS] restore default rate limit -> HTTP 200

Acceptance finished. Runtime record: docs\acceptance\runtime\last-gateway-acceptance.json
```

**截图点 S5**:

- S5-1:`admin updates rate limit to 5 -> HTTP 200`
- S5-2:`rate-limit request 1..5 -> HTTP 200`,然后 `rate-limit request 6 -> HTTP 429`
- S5-3:`[PASS] rate limit returned 429`
- S5-4:末尾 `Acceptance finished.`,且 JSON 摘要里 `adminRateLimitVerified=true`

**口播稿**:**"现在我们让管理员把文章详情接口的限流阈值调成 5:看到 200。然后连续请求 6 次同一个文章详情接口,前 5 次是 200,第 6 次是 429 — 限流触发。然后我们再调回默认值。这就是动态下发限流:不需要重启 gateway-service、不需要修改配置文件,网关的 `RateLimitAdminController` 直接生效。"**

---

### 分镜 6 / AI 续写 + SSE 流式(3:10 - 3:55)

**目的**:印证"AI 续写采用 SSE 流式响应,逐字/逐句推送给客户端"。

**操作 — 全手动 curl**,因为 acceptance-gateway.ps1 里面 SSE 是脚本执行看不到"逐字"效果的:

```powershell
# 重新拿一个普通用户 token
$body = '{"username":"a_accept_' + (Get-Date -Format "HHmmss") + '","password":"123456"}'
$regBody = $body   # 注册一次,等下也用
$login   = curl.exe -s -X POST -H "Content-Type: application/json" -d $body `
   "http://localhost:8080/api/user/login"
$tok = ($login | ConvertFrom-Json).data.token
Write-Host ("token length = " + $tok.Length)

# (1) 同步续写 — 一次性返回完整文本
$sync = curl.exe -s -X POST -H "Authorization: Bearer $tok" -H "Content-Type: application/json" `
   -d '{"prompt":"请写一段 Redis ZSET 的优势"}' `
   "http://localhost:8080/api/continue-writing"
# ↑ 注意:这里 URL 应该是 /api/ai/continue-writing,见下方修正
```

> 上面的 URL 是笔误,正确命令如下:

```powershell
# 同步:POST /api/ai/continue-writing
$sync = curl.exe -s -X POST `
   -H "Authorization: Bearer $tok" `
   -H "Content-Type: application/json" `
   -d '{"prompt":"请写一段 Redis ZSET 的优势"}' `
   "http://localhost:8080/api/ai/continue-writing"
Write-Host $sync

# 流式:GET /api/ai/continue-writing/stream?prompt=...
$promptUtf8 = [System.Web.HttpUtility]::UrlEncode("请写一段Redis ZSET的优势")
curl.exe -sN -H ("Authorization: Bearer " + $tok) `
   ("http://localhost:8080/api/ai/continue-writing/stream?prompt=" + $promptUtf8)
```

**截图点 S6**:

- S6-1:同步接口一次性返回的 JSON,`data.text` 是完整 Vo
- S6-2:**最关键的画面**:SSE 流式输出 — 终端滚动出现 **多个** `data: <chunk>` 帧,每帧间隔约 100ms,直到最后连接关闭
- S6-3(可选):录屏时同步打开浏览器或 Postman 调同一 SSE URL,客户端的"逐字增长"动画比终端更直观

**口播稿**:**"我们调 `/api/ai/continue-writing` 看同步响应,一次性把整段续写完。然后调 SSE 端点 `/api/ai/continue-writing/stream` — 你看到这个终端里 `data:` 一行一行地弹出,每行就是一段 chunk,服务端用 `SseEmitter` 异步分段推,~100ms 一个。客户端不需要轮询,服务端的每一段都实时吐给客户端,这正是现代 AI 产品的交互体验。本项目用 Mock LLM 演示,真实接入只要替换 `AiService` 内部生成即可。"**

---

### 分镜 7 / 收尾(3:55 - 4:10)

**操作**:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\stop-all-services.ps1
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**截图点 S7**:`Services stopped.` 的终端输出,以及 `docker ps` 还显示 3 个中间件 healthy(中间件不关)。

**口播稿(15 秒)**:**"系统架构:5 个微服务 + 3 个中间件;网关统一入口;Redis ZSET 热榜 + 网关动态限流;RabbitMQ Fanout 文章广播;SSE AI 流式续写。指导书要求的三条核心考核链路(网关鉴权、Redis ZSET 热榜、MQ 异步广播、AI 流式响应)都在 5 分钟里走完。运行时证据见 `docs/acceptance/runtime/last-gateway-acceptance.json`,运行时 ZSET 文本证据见 `docs/acceptance/ranking/b-redis-zset-evidence.txt`。完整 Postman 集合在 `postman/AI-KnowledgeHub.postman_collection.json`,源码与脚本在 origin/main。"**

---

## 3. 录屏要点(避免翻车)

| 坑 | 应对 |
|---|---|
| PowerShell 中文乱码 | 录屏前先 `chcp 65001`;中文字符串保持用反引号转义(`\"`)而非英文双引号 |
| Invoke-RestMethod -Headers hashtable 偶尔丢 Authorization 头 | **本脚本凡是发 HTTP,统一用 curl**(Windows 10+ 自带),除非必须才用 Invoke-RestMethod |
| 录到一半 Redis 挂了 | 录前 `docker ps` 三行 healthy;若 429 不触发则 `docker restart akh-redis` |
| 流式 SSE 看不出"逐字"效果 | 同时用浏览器或 Postman 打开 SSE URL,客户端的"逐字增长"动画比终端直观 |
| articleId 涨到 7、8、9 演示草稿时拿不到 1 | 接受这个事实,用最新返回的 `data.articleId` 即可,不影响演示连续性 |
| 上一个 PS session 的 `$login` / `$token` 残留导致错误码 | **新开 PowerShell 窗口**,完全干净 session,只跑本脚本命令 |
| `MYSQL_ROOT_PASSWORD` 变量不存在 | `.env.example` 默认空,所以 `${MYSQL_ROOT_PASSWORD:-}` 会用空串连接,直接 `docker exec akh-mysql mysql -uroot user_db` 也行 |
| **acceptance-gateway.ps1 一旦启动会顺次跑完所有 9 段、无法中断** | OBS 起双 PowerShell 窗口并排录制:**左窗口跑 `acceptance-gateway.ps1`、右窗口做手动操作**,两个窗口并行。具体在分镜 3 / 4 / 5 都有体现 |
| **`curl` 在 PowerShell 5.1 里是 `Invoke-WebRequest` 的别名** | 文档里所有 `curl` 实际指 `curl.exe`(Windows 10 1803+ 自带)。`curl -H "..."`(无 .exe 后缀)会被 alias 解成 `Invoke-WebRequest -Headers`(期望 hashtable),从而报"无法将 System.String 转换为 IDictionary"。**所有 HTTP 调用显式写 `curl.exe`** |
| **PS 5.1 反引号 JSON 和 `ConvertTo-Json` 输出传给 `curl.exe -d` 都可能引入非法字节** | 反引号转义被 tokenizer 错位;`ConvertTo-Json -Compress` 输出在传给外部命令时 PS 5.1 可能附加 BOM/编码。**凡是 JSON body 含 `$变量` 插值,统一用单引号字符串拼接**: `'{"key":"' + $var + '"}'`。纯常量 JSON 用单引号 `'{"key":"val"}'` 仍然安全 |

---

## 4. 关键截图清单

| # | 必截内容 | 落盘路径(可选) |
|---|---|---|
| S1 | 3 个中间件 healthy + 5 个端口 LISTENING | 录屏自带 |
| S2 | 6 个 [PASS] (网关 / 注册 / 登录 / 401 / 200 / 403) | 录屏自带 |
| S3 | 8 个 [PASS] (article + AI) + RabbitMQ Management 后台画面 | `docs/screenshots/acceptance/7-mq-management.png` |
| S4 | redis-cli ZREVRANGE + HTTP data.total 一致 | `docs/screenshots/acceptance/8-redis-zset.png` |
| S5 | rate-limit request 1..6 + 429 + 恢复 | `docs/screenshots/acceptance/9-rate-limit-429.png` |
| S6 | SSE 流式 5+ 个 `data:` 帧 | 录屏特写 |
| S7 | Services stopped + clean desktop | 录屏自带 |

---

## 5. 重置 / 重跑流程(录像中途出错时)

```powershell
# (1) 清场:停 5 个 Java 服务
powershell -ExecutionPolicy Bypass -File scripts\stop-all-services.ps1

# (2) 重启
powershell -ExecutionPolicy Bypass -File scripts\start-all-services.ps1

# (3) 若 Redis 容器出问题
docker restart akh-redis

# (4) 若 article.id 已经涨到很大,想从 1 开始
docker exec akh-mysql mysql -uroot user_db -e "TRUNCATE article;"

# (5) 清掉 JWT 黑名单 + 限流配置
redis-cli -h localhost FLUSHDB
```

---

## 6. 与现有素材的对应

| 现有资产 | 在本脚本中的角色 |
|---|---|
| `scripts/acceptance-gateway.ps1` | 分镜 2、3、5 的核心触发器 |
| `scripts/start-all-services.ps1` / `stop-all-services.ps1` | 分镜 0、分镜 7 开关 |
| `docs/acceptance/A-gateway-unified-entry-acceptance.md` | A 撰写的英文验收指南,本脚本是其视频版本 |
| `docs/acceptance/runtime/last-gateway-acceptance.json` | acceptance-gateway.ps1 落地的 JSON,视频录制后引用即可 |
| `docs/acceptance/ranking/b-redis-zset-evidence.txt` | 分镜 4 的文字证据,与录屏一对一 |
| `postman/AI-KnowledgeHub.postman_collection.json` | 视频外,给老师/助教复习用 |
| `Web后端-AI-KnowledgeHub-课程设计报告.md` §3.4 / §6.3 / §9.4.3 | 报告章节与本视频一一对应 |

---

## 7. 录屏前最后一次自检清单

```
[ ] docker ps 三行 healthy
[ ] 5 个 Java 端口 LISTENING
[ ] curl.exe /actuator/health 200
[ ] acceptance-gateway.ps1(不带 -AdminToken)13 个 PASS,1 个 SKIP
[ ] acceptance-gateway.ps1 -AdminToken <token> 全 17 个 PASS,无 SKIP
[ ] redis-cli ZREVRANGE article:hot:ranking 0 9 WITHSCORES 有输出
[ ] curl.exe /api/ai/continue-writing/stream?prompt=... 出现至少 3 个 data: 帧
[ ] OBS 场景已切到 PowerShell 终端
[ ] 输出窗口最大化和 UTF-8(chcp 65001)
[ ] 音频设备 OK
```

全部打钩,开始录制。
