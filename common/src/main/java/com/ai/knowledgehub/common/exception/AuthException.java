package com.ai.knowledgehub.common.exception;

import com.ai.knowledgehub.common.result.ResultCode;

/**
 * 鉴权相关业务异常
 * <p>
 * 用于登录态失效、Token 无效、权限不足等场景。
 * 网关层的 {@code AuthGlobalFilter} 抛出该异常，
 * 由 {@link GlobalExceptionHandler} 统一转换为 401 / 403 响应。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
public class AuthException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用鉴权相关错误码构造异常
     *
     * @param resultCode 错误码枚举（如 UNAUTHORIZED / FORBIDDEN / TOKEN_INVALID）
     */
    public AuthException(ResultCode resultCode) {
        super(resultCode);
    }

    /**
     * 使用鉴权相关错误码 + 自定义消息构造异常
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误消息
     */
    public AuthException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * 快速创建：未授权
     */
    public static AuthException unauthorized() {
        return new AuthException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 快速创建：Token 无效
     */
    public static AuthException tokenInvalid() {
        return new AuthException(ResultCode.TOKEN_INVALID);
    }

    /**
     * 快速创建：Token 已过期
     */
    public static AuthException tokenExpired() {
        return new AuthException(ResultCode.TOKEN_EXPIRED);
    }

    /**
     * 快速创建：无权限访问
     */
    public static AuthException forbidden() {
        return new AuthException(ResultCode.FORBIDDEN);
    }
}
