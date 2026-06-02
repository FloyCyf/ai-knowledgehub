# article-service

文章服务负责文章内容管理、评论和点赞。

## 核心接口

- `POST /api/articles/draft`
- `PUT /api/articles/{id}`
- `POST /api/articles/{id}/publish`
- `DELETE /api/articles/{id}`
- `GET /api/articles/latest`
- `GET /api/articles/{id}`
- `POST /api/articles/{id}/like`
- `POST /api/articles/{id}/comments`
- `GET /api/articles/{id}/comments`

## 数据表

- `article`
- `comment`
- `article_like`

## MQ 事件

发布文章后发送：

```text
ArticlePublishedEvent
```

