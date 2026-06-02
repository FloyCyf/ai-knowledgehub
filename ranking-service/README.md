# ranking-service

热榜服务负责文章热度维护和 Top10 查询。

## 核心接口

- `POST /api/ranking/articles/{id}/view`
- `POST /api/ranking/articles/{id}/like`
- `GET /api/ranking/top10`

## Redis ZSET

```text
article:hot:ranking
```

热度规则：

- 阅读文章：+1。
- 发布文章：+2。
- 评论文章：+3。
- 点赞文章：+5。

