# common — 公共模块

`common` 是整个 AI-KnowledgeHub 项目的公共能力库，被所有 5 个微服务依赖。
**注意**：本模块基于 Spring MVC 编写，被 `gateway-service`（WebFlux）依赖时会排除
`spring-boot-starter-web` / `spring-boot-starter-validation` / Knife4j。

## 目录结构

```
common/
├── constant/          业务常量
│   └── HeaderConstants.java       请求头常量（X-User-Id / X-User-Role / 角色）
├── dto/               通用 DTO
│   ├── PageRequest.java           统一分页请求（page / size，默认 1/10）
│   └── PageResult.java            统一分页响应（list / total / page / size / totalPages）
├── entity/            通用实体
│   └── BaseEntity.java            基础实体（含 id / createTime / updateTime / createBy / updateBy / deleted）
├── exception/         通用异常
│   ├── BusinessException.java     业务异常基类
│   ├── AuthException.java         鉴权异常（401/403 统一入口）
│   └── GlobalExceptionHandler.java 全局异常处理器
├── result/            统一返回
│   ├── ApiResponse.java           统一返回结构 {code, message, data, timestamp}
│   └── ResultCode.java            统一错误码枚举
├── util/              工具类
│   └── SecurityUtils.java         当前登录用户上下文（从 X-User-Id 头获取）
├── config/            配置类
│   ├── JwtProperties.java         JWT 配置项
│   ├── JwtUtil.java               JWT 生成/解析/校验工具
│   ├── JwtAutoConfiguration.java  JWT 自动配置
│   ├── MybatisPlusConfig.java     MyBatis-Plus 分页插件
│   ├── MybatisMetaObjectHandler.java 自动填充处理器
│   └── MetaObjectHandlerAutoConfiguration.java MetaObjectHandler 自动注册
└── swagger/           API 文档
    └── SwaggerConfig.java         Knife4j 基础配置（含 JWT 鉴权按钮）
```

## 关键约定

### 1. 统一返回结构

所有 Controller 必须返回 `ApiResponse<T>`，**不**用 `Map` 拼装：

```java
@GetMapping("/profile")
public ApiResponse<UserVO> profile() {
    UserVO user = ...;
    return ApiResponse.success(user);
}
```

### 2. 业务异常

抛 `BusinessException` 或子类（`AuthException`），由 `GlobalExceptionHandler` 统一处理：

```java
if (user == null) {
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}

if (SecurityUtils.getCurrentUserId() == null) {
    throw AuthException.unauthorized();
}
```

### 3. 鉴权上下文

从网关透传的 `X-User-Id` / `X-User-Role` 头取用户信息：

```java
Long userId = SecurityUtils.getCurrentUserId();           // 可空
Long userId = SecurityUtils.requireCurrentUserId();        // 未登录抛 AuthException
String role = SecurityUtils.getCurrentUserRole();
SecurityUtils.requireAdmin();                              // 非管理员抛 AuthException
```

### 4. 分页请求

所有分页接口接收 `PageRequest`，返回 `PageResult`：

```java
@GetMapping("/list")
public ApiResponse<PageResult<ArticleVO>> list(PageRequest pageRequest) {
    return ApiResponse.success(articleService.page(pageRequest));
}
```

### 5. 实体继承 BaseEntity

业务实体必须 `extends BaseEntity` 以获得自动填充：

```java
@Data
@TableName("user")
public class User extends BaseEntity {
    private String username;
    private String passwordHash;
    private String role;
    private String status;
}
```

`MybatisMetaObjectHandler` 会自动填充：
- INSERT：`createTime` / `updateTime` / `createBy` / `updateBy`
- UPDATE：`updateTime` / `updateBy`

### 6. JWT 签发

```java
// 业务 Service 中
String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
```

密钥 / 过期时间从 `application.yml` 的 `jwt.*` 读取（由 `JwtAutoConfiguration` 自动注入）。

### 7. 错误码

`ResultCode` 枚举中已定义所有错误码，**不**允许在代码中硬编码数字：
- 200：成功
- 400：参数错误
- 401：未登录 / Token 无效
- 403：无权限
- 404：资源不存在
- 409：数据冲突（如重复点赞）
- 422：参数验证失败
- 429：限流
- 500：服务器错误
- 1000-1999：通用业务错误
- 2000-2999：用户相关错误
- 3000-3999：文章相关错误
- 4000-4999：AI 服务错误
- 5000-5999：文件相关错误

## 注意事项

1. **不要**在 `common` 模块中写业务代码
2. **不要**在 `common` 模块中加 Spring WebFlux 依赖
3. **不要**修改 `ApiResponse` 字段，所有服务都依赖 `{code, message, data, timestamp}`
4. **不要**绕过 `GlobalExceptionHandler` 在 Controller 中 try-catch 业务异常
5. **不要**返回 `Map<String, Object>` 模拟 API 响应

## 依赖版本

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 3.2.5 | 见根 pom |
| Spring Cloud | 2023.0.1 | 见根 pom |
| MyBatis-Plus | 3.5.5 | 见根 pom |
| jjwt | 0.12.5 | 见根 pom |
| Knife4j | 4.4.0 | 见根 pom |
