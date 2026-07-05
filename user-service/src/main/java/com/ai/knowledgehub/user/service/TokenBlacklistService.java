package com.ai.knowledgehub.user.service;

import com.ai.knowledgehub.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Token 黑名单服务（注销登录使用）
 * <p>
 * 将已注销的 token jti 写入 Redis，TTL 等于 token 剩余有效期。
 * 网关层（阶段 3）会调用 {@link #isBlacklisted(String)} 拦截黑名单中的 token。
 * </p>
 *
 * <p>Redis Key 设计：</p>
 * <pre>
 *   token:blacklist:{jti}
 * </pre>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /**
     * Redis Key 前缀（与 docs/coding-standards.md 第 55 行约定一致）
     */
    public static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Autowired
    public TokenBlacklistService(StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 将 Token 加入黑名单
     *
     * @param claims Token 解析后的 Claims
     */
    public void blacklist(Claims claims) {
        if (claims == null) {
            return;
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            // 老 Token 没有 jti，用 subject(username) 作为 key
            jti = claims.getSubject();
        }
        if (jti == null || jti.isBlank()) {
            log.warn("Token 无 jti 和 subject，跳过黑名单");
            return;
        }

        Date expiration = claims.getExpiration();
        if (expiration == null) {
            // 没有过期时间，按默认 24h 兜底
            expiration = Date.from(Instant.now().plusMillis(jwtProperties.getExpirationMillis()));
        }

        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            log.debug("Token 已过期，无需加入黑名单: jti={}", jti);
            return;
        }

        String key = BLACKLIST_KEY_PREFIX + jti;
        try {
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofMillis(ttlMillis));
            log.info("Token 已加入黑名单: jti={}, ttl={}ms", jti, ttlMillis);
        } catch (Exception e) {
            log.warn("Redis 不可用，注销黑名单写入跳过: jti={}, reason={}", jti, e.getMessage());
        }
    }

    /**
     * 校验 Token 是否在黑名单中
     *
     * @param jti JWT ID
     * @return true=已注销（需拒绝）
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            Boolean exists = stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过 Token 黑名单校验: jti={}, reason={}", jti, e.getMessage());
            return false;
        }
    }
}
