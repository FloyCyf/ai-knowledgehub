package com.ai.knowledgehub.common.util;

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

/**
 * JWT 工具类
 * <p>
 * 提供 JWT Token 的生成、解析、验证等功能
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
public class JwtUtil {

    /**
     * 默认密钥（建议在生产环境中使用配置文件注入）
     */
    private static final String DEFAULT_SECRET = "ai-knowledgehub-default-secret-key-for-jwt-token-generation-2024";

    /**
     * 默认过期时间（24小时，单位：毫秒）
     */
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 请求头名称
     */
    public static final String HEADER_NAME = "Authorization";

    /**
     * 用户ID声明名称
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
     * 生成密钥
     *
     * @param secret 密钥字符串
     * @return SecretKey 对象
     */
    private static SecretKey getSecretKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token（使用默认配置）
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     用户角色
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    /**
     * 生成 Token（自定义密钥和过期时间）
     *
     * @param userId     用户ID
     * @param username   用户名
     * @param role       用户角色
     * @param secret     密钥
     * @param expiration 过期时间（毫秒）
     * @return JWT Token
     */
    public static String generateToken(Long userId, String username, String role, String secret, long expiration) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_USERNAME, username);
        claims.put(CLAIM_ROLE, role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey(secret))
                .compact();
    }

    /**
     * 解析 Token
     *
     * @param token JWT Token
     * @return Claims 对象
     */
    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    /**
     * 解析 Token（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return Claims 对象
     */
    public static Claims parseToken(String token, String secret) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey(secret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("Token 签名无效: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Token 格式错误: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Token 解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, DEFAULT_SECRET);
    }

    /**
     * 验证 Token 是否有效（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 是否有效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token JWT Token
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token) {
        return isTokenExpired(token, DEFAULT_SECRET);
    }

    /**
     * 检查 Token 是否过期（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 是否过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = parseToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        return getUserId(token, DEFAULT_SECRET);
    }

    /**
     * 从 Token 中获取用户ID（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 用户ID
     */
    public static Long getUserId(String token, String secret) {
        Claims claims = parseToken(token, secret);
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public static String getUsername(String token) {
        return getUsername(token, DEFAULT_SECRET);
    }

    /**
     * 从 Token 中获取用户名（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 用户名
     */
    public static String getUsername(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Token 中获取用户角色
     *
     * @param token JWT Token
     * @return 用户角色
     */
    public static String getRole(String token) {
        return getRole(token, DEFAULT_SECRET);
    }

    /**
     * 从 Token 中获取用户角色（自定义密钥）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 用户角色
     */
    public static String getRole(String token, String secret) {
        Claims claims = parseToken(token, secret);
        return claims.get(CLAIM_ROLE, String.class);
    }

    /**
     * 从请求头中提取 Token
     *
     * @param authHeader Authorization 请求头值
     * @return JWT Token（不含 Bearer 前缀）
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 刷新 Token（生成新的 Token）
     *
     * @param token 旧 Token
     * @return 新 Token
     */
    public static String refreshToken(String token) {
        return refreshToken(token, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    /**
     * 刷新 Token（生成新的 Token，自定义密钥和过期时间）
     *
     * @param token      旧 Token
     * @param secret     密钥
     * @param expiration 过期时间（毫秒）
     * @return 新 Token
     */
    public static String refreshToken(String token, String secret, long expiration) {
        Claims claims = parseToken(token, secret);
        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String username = claims.get(CLAIM_USERNAME, String.class);
        String role = claims.get(CLAIM_ROLE, String.class);
        return generateToken(userId, username, role, secret, expiration);
    }
}