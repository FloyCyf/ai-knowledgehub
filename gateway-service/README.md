# gateway-service

网关服务负责所有请求的统一入口。

## 主要职责

- 路由转发。
- JWT Token 校验。
- 普通用户和管理员权限判断。
- IP 限流。
- 从 Nacos 配置中心动态读取限流配置，Redis 仅用于限流计数。

## 路由表

所有外部请求统一从 `http://localhost:8080` 进入，网关保留完整 `/api` 前缀转发给下游服务。

| 路径 | 下游服务 | 说明 |
| --- | --- | --- |
| `/api/user/**` | `user-service:8081` | 注册、登录、注销、个人信息 |
| `/api/articles/**` | `article-service:8082` | 文章、评论、点赞 |
| `/api/ranking/**` | `ranking-service:8083` | 热榜 |
| `/api/ai/**` | `ai-service:8084` | AI 续写、SSE、分析结果 |
| `/api/admin/rate-limit/article-detail` | gateway-service | 网关内部动态限流管理接口 |

## 鉴权规则

- 白名单：`POST /api/user/register`、`POST /api/user/login`、`GET /api/articles/latest`、`GET /api/articles/{id}`、`GET /api/ranking/top10`、`/actuator/**`。
- 其他接口必须携带 `Authorization: Bearer <token>`。
- 网关解析 JWT 后覆盖客户端传入的 `X-User-Id`、`X-User-Role`、`X-User-Name`，只信任 Token 中的用户上下文。
- `/api/admin/**` 仅允许 `ADMIN` 角色访问，普通用户返回 403。

## 重点接口

- `PUT /api/admin/rate-limit/article-detail`

## Nacos 限流配置

网关从 Nacos `gateway-service.yml` 动态读取文章详情限流配置。本地默认值仍保留在 `application.yml` 中，用于 Nacos 不可用时启动兜底。

```yaml
rate-limit:
  enabled: true
  article-detail:
    window-seconds: 10
    max-requests: 20
```

Redis 只保存固定窗口计数 key，例如：

```text
rate_limit:{ip}:/api/articles/{id}
```

## 限流演示

```bash
# 文章详情默认 10 秒最多 20 次，第 21 次返回 429
for i in {1..25}; do
  curl -s -o /dev/null -w "request $i -> %{http_code}\n" \
    http://localhost:8080/api/articles/1
done

# 管理员通过网关发布配置到 Nacos
curl -X PUT http://localhost:8080/api/admin/rate-limit/article-detail \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"windowSeconds":10,"maxRequests":5,"enabled":true}'
```

## 统一入口验收

项目根目录提供了 A 负责的网关验收脚本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1
```

带管理员 Token 时可以完整验证动态限流配置和 429：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-gateway.ps1 -AdminToken "<admin-token>"
```

也可以单独验证 Nacos 动态配置中心限流：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\acceptance-nacos-rate-limit.ps1 -AdminToken "<admin-token>"
```

详细步骤和截图清单见 `docs/acceptance/A-gateway-unified-entry-acceptance.md`。
