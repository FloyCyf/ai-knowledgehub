# ai-service

AI 服务负责 AI 续写、MQ 消费、标签提取和合规检测。

## 核心接口

- `GET /api/ai/continue-writing/stream`
- `POST /api/ai/continue-writing`
- `GET /api/ai/articles/{id}/analysis`

## MQ 消费

- `article.tag.queue`：AI 标签提取消费者。
- `article.audit.queue`：AI 合规检测消费者。

## 数据表

- `article_ai_tag`
- `article_audit_result`

## 本地实现建议

真实 LLM API 不稳定时，优先使用 Mock LLM，报告中说明生产环境可替换为真实 LLM API。

