package com.ai.knowledgehub.common.constant;

/**
 * 请求头常量类
 * <p>
 * 定义系统中使用的请求头常量，主要用于服务间传递用户信息
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
public final class HeaderConstants {

    private HeaderConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 用户ID请求头
     */
    public static final String X_USER_ID = "X-User-Id";

    /**
     * 用户角色请求头
     */
    public static final String X_USER_ROLE = "X-User-Role";

    /**
     * 用户名请求头
     */
    public static final String X_USER_NAME = "X-User-Name";

    /**
     * Token 请求头
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    public static final String BEARER = "Bearer ";

    /**
     * 请求来源服务头
     */
    public static final String X_SOURCE_SERVICE = "X-Source-Service";

    /**
     * 请求追踪ID头
     */
    public static final String X_TRACE_ID = "X-Trace-Id";

    /**
     * 请求ID头
     */
    public static final String X_REQUEST_ID = "X-Request-Id";

    // ==================== 用户角色常量 ====================

    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * 普通用户角色
     */
    public static final String ROLE_USER = "USER";

    /**
     * VIP用户角色
     */
    public static final String ROLE_VIP = "VIP";
}