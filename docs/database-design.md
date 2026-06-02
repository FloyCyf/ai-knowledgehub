# 数据库表设计建议

数据库建议使用 MySQL。Redis 主要用于热榜、限流计数、缓存和 Token 黑名单。

## user 用户表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| username | varchar(64) | 用户名，唯一 |
| password_hash | varchar(255) | 加密后的密码 |
| role | varchar(32) | USER / ADMIN |
| status | varchar(32) | ENABLED / DISABLED |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## article 文章表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| author_id | bigint | 作者 ID |
| title | varchar(255) | 标题 |
| content | text | 正文 |
| summary | varchar(500) | 摘要 |
| status | varchar(32) | DRAFT / PUBLISHED |
| view_count | bigint | 阅读数 |
| like_count | bigint | 点赞数 |
| comment_count | bigint | 评论数 |
| deleted | tinyint | 逻辑删除标记 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |
| published_at | datetime | 发布时间 |

## comment 评论表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| article_id | bigint | 文章 ID |
| user_id | bigint | 用户 ID |
| content | varchar(1000) | 评论内容 |
| deleted | tinyint | 逻辑删除标记 |
| created_at | datetime | 创建时间 |

## article_like 点赞表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| article_id | bigint | 文章 ID |
| user_id | bigint | 用户 ID |
| created_at | datetime | 创建时间 |

唯一约束：

```text
article_id + user_id
```

用于防止同一用户重复点赞。

## article_ai_tag AI 标签表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| article_id | bigint | 文章 ID |
| tags | varchar(500) | 标签列表，可用逗号或 JSON 存储 |
| model_name | varchar(100) | 模型名称或 Mock 标识 |
| created_at | datetime | 创建时间 |

## article_audit_result AI 合规检测表

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| article_id | bigint | 文章 ID |
| result | varchar(32) | PASS / REVIEW / REJECT |
| reason | varchar(1000) | 检测原因 |
| model_name | varchar(100) | 模型名称或 Mock 标识 |
| created_at | datetime | 创建时间 |

## rate_limit_rule 限流规则表，可选

如果希望从 MySQL 管理限流规则，可以增加该表。若降低实现难度，也可以直接将限流配置放入 Redis Hash。

| 字段 | 类型建议 | 说明 |
| --- | --- | --- |
| id | bigint | 主键 |
| rule_name | varchar(100) | 规则名称 |
| path_pattern | varchar(255) | 路径匹配规则 |
| window_seconds | int | 时间窗口秒数 |
| max_requests | int | 最大请求数 |
| enabled | tinyint | 是否启用 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## Redis Key 设计

### 热榜 ZSET

```text
article:hot:ranking
```

- member：articleId
- score：hotScore

热度规则建议：

| 行为 | 分值 |
| --- | --- |
| 阅读文章 | +1 |
| 发布文章 | +2 |
| 评论文章 | +3 |
| 点赞文章 | +5 |

### 网关限流计数

```text
rate_limit:{ip}:{path}
```

示例：

```text
rate_limit:127.0.0.1:/api/articles/1001
```

过期时间：10 秒。

### 动态限流配置

```text
rate_limit_config:article_detail
```

字段：

```text
windowSeconds = 10
maxRequests = 20
enabled = true
```

### Token 黑名单，可选

```text
token:blacklist:{tokenId}
```

用于注销后 Token 失效控制。

