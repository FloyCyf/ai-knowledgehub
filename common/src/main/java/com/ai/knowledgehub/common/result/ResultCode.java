package com.ai.knowledgehub.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * <p>
 * 定义系统常用的错误码和错误消息
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // ==================== 成功状态 ====================
    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    // ==================== 客户端错误 4xx ====================
    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未授权，请先登录
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 无权限访问
     */
    FORBIDDEN(403, "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不允许
     */
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    /**
     * 请求参数验证失败
     */
    PARAM_VALID_ERROR(422, "请求参数验证失败"),

    // ==================== 服务端错误 5xx ====================
    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // ==================== 业务错误 1xxx ====================
    /**
     * 业务异常
     */
    BUSINESS_ERROR(1000, "业务处理异常"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(1001, "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(1002, "数据不存在"),

    /**
     * 数据操作失败
     */
    DATA_OPERATION_FAILED(1003, "数据操作失败"),

    // ==================== 用户相关错误 2xxx ====================
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(2000, "用户不存在"),

    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(2001, "用户名或密码错误"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(2002, "用户已被禁用"),

    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(2003, "用户名已存在"),

    /**
     * 手机号已存在
     */
    PHONE_EXISTS(2004, "手机号已存在"),

    /**
     * 邮箱已存在
     */
    EMAIL_EXISTS(2005, "邮箱已存在"),

    /**
     * 原密码错误
     */
    OLD_PASSWORD_ERROR(2006, "原密码错误"),

    /**
     * Token 无效
     */
    TOKEN_INVALID(2007, "Token 无效或已过期"),

    /**
     * Token 已过期
     */
    TOKEN_EXPIRED(2008, "Token 已过期"),

    // ==================== 文章相关错误 3xxx ====================
    /**
     * 文章不存在
     */
    ARTICLE_NOT_FOUND(3000, "文章不存在"),

    /**
     * 文章已删除
     */
    ARTICLE_DELETED(3001, "文章已删除"),

    /**
     * 文章审核中
     */
    ARTICLE_UNDER_REVIEW(3002, "文章审核中"),

    /**
     * 文章已发布
     */
    ARTICLE_ALREADY_PUBLISHED(3003, "文章已发布"),

    // ==================== AI服务相关错误 4xxx ====================
    /**
     * AI服务调用失败
     */
    AI_SERVICE_ERROR(4000, "AI服务调用失败"),

    /**
     * AI服务超时
     */
    AI_SERVICE_TIMEOUT(4001, "AI服务超时"),

    /**
     * AI配额不足
     */
    AI_QUOTA_INSUFFICIENT(4002, "AI配额不足"),

    // ==================== 文件相关错误 5xxx ====================
    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(5000, "文件上传失败"),

    /**
     * 文件大小超限
     */
    FILE_SIZE_EXCEEDED(5001, "文件大小超限"),

    /**
     * 文件类型不支持
     */
    FILE_TYPE_NOT_SUPPORTED(5002, "文件类型不支持"),

    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(5003, "文件不存在");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;
}