package com.ai.knowledgehub.common.config;

import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类
 * <p>
 * 通过 {@link JwtProperties} 注入密钥与过期时间，
 * 各服务通过 {@code @EnableConfigurationProperties(JwtProperties.class)} 启用。
 * </p>
 *
 * <p>向后兼容：保留静态方法（使用 {@link JwtProperties#DEFAULT_SECRET}），
 * 供单元测试 / 未启用配置的服务使用。</p>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
public class JwtUtil {

    /**
     * 静态方法使用的默认实例（延迟初始化）
     */
    private static volatile JwtProperties defaultProperties;

    /**
     * 用户 ID 声明名称
     */
    public static final String CLAIM_USER_ID = "userId";

    /**
     * 用户名声明名称
     */
    public static final String CLAIM_USERNAME = "username";

    /**
     * 用户角色声明名称
     */
    public static final String CLAIM_ROLE = "role";

    /**
     * 设置默认配置（由业务模块启动时调用）
     *
     * @param properties JWT 配置
     */
    public static void setDefaultProperties(JwtProperties properties) {
        defaultProperties = properties;
    }

    /**
     * 获取当前生效的 JwtProperties
     */
    private static JwtProperties getProperties() {
        if (defaultProperties == null) {
            defaultProperties = new JwtProperties();
        }
        return defaultProperties;
    }

    /**
     * 生成密钥
     */
    private static SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ============================================================
    // 实例方法（推荐使用）
    // ============================================================

    /**
     * 使用指定配置生成 Token
     *
     * @param userId     用户 ID
     * @param username   用户名
     * @param role       角色
     * @param properties 配置
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, String role, JwtProperties properties) {
        properties.validate();
        Map<String, Object> claims = new HashMap<>(4);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE, role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + properties.getExpirationMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(properties.getIssuer())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey(properties.getSecret()))
                .compact();
    }

    /**
     * 使用默认配置生成 Token
     */
    public static String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, getProperties());
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token, JwtProperties properties) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey(properties.getSecret()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw new AuthException(ResultCode.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            log.warn("Token 签名无效: {}", e.getMessage());
            throw new AuthException(ResultCode.TOKEN_INVALID, "Token 签名无效");
        } catch (MalformedJwtException e) {
            log.warn("Token 格式错误: {}", e.getMessage());
            throw new AuthException(ResultCode.TOKEN_INVALID, "Token 格式错误");
        } catch (Exception e) {
            log.error("Token 解析失败: {}", e.getMessage());
            throw new AuthException(ResultCode.TOKEN_INVALID);
        }
    }

    /**
     * 使用默认配置解析 Token
     */
    public static Claims parseToken(String token) {
        return parseToken(token, getProperties());
    }

    /**
     * 校验 Token 是否有效
     */
    public static boolean validateToken(String token, JwtProperties properties) {
        try {
            parseToken(token, properties);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validateToken(String token) {
        return validateToken(token, getProperties());
    }

    // ============================================================
    // 便捷方法
    // ============================================================

    /**
     * 从请求头中提取 Token（去除 "Bearer " 前缀）
     *
     * @param authHeader Authorization 头值
     * @return 纯 Token 字符串，若格式错误返回 null
     */
    public static String extractToken(String authHeader) {
        String prefix = getProperties().getTokenPrefix();
        if (authHeader != null && authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length());
        }
        return null;
    }

    /**
     * 从 Claims 中获取 userId（兼容 Integer / Long）
     */
    public static Long getUserId(Claims claims) {
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    /**
     * 从 Claims 中获取 username
     */
    public static String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Claims 中获取 role
     */
    public static String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    /**
     * 刷新 Token
     */
    public static String refreshToken(String token, JwtProperties properties) {
        Claims claims = parseToken(token, properties);
        Long userId = getUserId(claims);
        String username = getUsername(claims);
        String role = getRole(claims);
        return generateToken(userId, username, role, properties);
    }

    public static String refreshToken(String token) {
        return refreshToken(token, getProperties());
    }
}
