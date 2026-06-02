# gateway-service

网关服务负责所有请求的统一入口。

## 主要职责

- 路由转发。
- JWT Token 校验。
- 普通用户和管理员权限判断。
- IP 限流。
- 从 Redis 动态读取限流配置。

## 重点接口

- `PUT /api/admin/rate-limit/article-detail`

## Redis 限流配置

```text
rate_limit_config:article_detail
windowSeconds = 10
maxRequests = 20
enabled = true
```

