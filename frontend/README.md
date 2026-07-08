# AI-KnowledgeHub 期末验收前端

这是一个无构建静态单页前端，用来演示 AI-KnowledgeHub 的核心后端接口。

## 使用方式

1. 按项目根目录 README 启动 MySQL、Redis、RabbitMQ 和 5 个后端服务。
2. 确认网关服务可访问：`http://localhost:8080`。
3. 直接用浏览器打开 `frontend/index.html`。
4. 如果网关端口改过，在页面右上角修改“网关地址”并保存。

## 演示顺序

1. 注册或登录用户。
2. 创建文章草稿。
3. 发布当前文章。
4. 刷新最新文章列表并查看详情。
5. 点赞、发表评论、查看评论。
6. 刷新排行榜 Top10。
7. 演示 AI 普通续写和 SSE 流式续写。

页面会把最近一次接口响应展示在“响应记录”区域，方便期末验收时说明请求结果。

## 说明

- 默认通过网关 `http://localhost:8080` 访问接口。
- Token 存在浏览器 `localStorage` 中，登录后请求会自动携带 `Authorization: Bearer <token>`。
- 前端不使用 `/api/tags/**`，因为当前网关配置没有转发标签接口。
- 不需要安装 Node.js，也不需要运行打包命令。
