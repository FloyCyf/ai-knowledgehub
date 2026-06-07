package com.ai.knowledgehub.common.util;

import com.ai.knowledgehub.common.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 安全上下文工具类
 * <p>
 * 提供从当前请求中获取用户信息（X-User-Id / X-User-Role）的能力。
 * 网关层将用户上下文写入请求头后，业务服务即可通过该工具类获取。
 * </p>
 *
 * <p>使用场景：</p>
 * <pre>
 *   // 业务方法中获取当前登录用户 ID
 *   Long userId = SecurityUtils.getCurrentUserId();
 *
 *   // 判断是否为管理员
 *   if (SecurityUtils.isAdmin()) { ... }
 * </pre>
 *
 * @author AI KnowledgeHub Team
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取当前登录用户 ID
     *
     * @return 用户 ID，若未登录返回 null
     */
    public static Long getCurrentUserId() {
        return getCurrentRequest()
                .map(req -> parseLong(req.getHeader(HeaderConstants.X_USER_ID)))
                .orElse(null);
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出鉴权异常
     *
     * @return 用户 ID
     * @throws com.ai.knowledgehub.common.exception.AuthException 若未登录
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw com.ai.knowledgehub.common.exception.AuthException.unauthorized();
        }
        return userId;
    }

    /**
     * 获取当前登录用户名
     *
     * @return 用户名
     */
    public static String getCurrentUsername() {
        return getCurrentRequest()
                .map(req -> req.getHeader(HeaderConstants.X_USER_NAME))
                .orElse(null);
    }

    /**
     * 获取当前登录用户角色
     *
     * @return 角色（USER / ADMIN）
     */
    public static String getCurrentUserRole() {
        return getCurrentRequest()
                .map(req -> req.getHeader(HeaderConstants.X_USER_ROLE))
                .orElse(null);
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return HeaderConstants.ROLE_ADMIN.equals(getCurrentUserRole());
    }

    /**
     * 要求当前用户是管理员，否则抛出鉴权异常
     *
     * @throws com.ai.knowledgehub.common.exception.AuthException 若非管理员
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw com.ai.knowledgehub.common.exception.AuthException.forbidden();
        }
    }

    /**
     * 获取当前 HTTP 请求（基于 Spring 的 RequestContextHolder）
     */
    private static Optional<HttpServletRequest> getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.ofNullable(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    /**
     * 安全地解析 Long 类型 Header
     */
    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
