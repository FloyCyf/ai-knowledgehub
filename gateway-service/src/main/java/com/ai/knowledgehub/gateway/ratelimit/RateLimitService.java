package com.ai.knowledgehub.gateway.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Redis 动态限流配置服务。
 */
@Slf4j
@Service
public class RateLimitService {

    public static final String ARTICLE_DETAIL_CONFIG_KEY = "rate_limit_config:article_detail";

    private static final String FIELD_WINDOW_SECONDS = "windowSeconds";
    private static final String FIELD_MAX_REQUESTS = "maxRequests";
    private static final String FIELD_ENABLED = "enabled";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitService(ReactiveStringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public RateLimitConfig defaultConfig() {
        return new RateLimitConfig(
                properties.getArticleDetail().getWindowSeconds(),
                properties.getArticleDetail().getMaxRequests(),
                properties.isEnabled()
        );
    }

    public Mono<RateLimitConfig> getArticleDetailConfig() {
        RateLimitConfig fallback = defaultConfig();
        return redisTemplate.opsForHash()
                .entries(ARTICLE_DETAIL_CONFIG_KEY)
                .collectMap(entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue()))
                .map(values -> values.isEmpty() ? fallback : fromMap(values, fallback))
                .onErrorResume(e -> {
                    log.warn("读取限流配置失败，使用默认配置并放行优先: {}", e.getMessage());
                    return Mono.just(fallback);
                });
    }

    public Mono<RateLimitConfig> updateArticleDetailConfig(RateLimitConfig config) {
        Map<String, String> values = Map.of(
                FIELD_WINDOW_SECONDS, String.valueOf(config.windowSeconds()),
                FIELD_MAX_REQUESTS, String.valueOf(config.maxRequests()),
                FIELD_ENABLED, String.valueOf(config.enabled())
        );
        return redisTemplate.opsForHash()
                .putAll(ARTICLE_DETAIL_CONFIG_KEY, values)
                .thenReturn(config);
    }

    private RateLimitConfig fromMap(Map<String, String> values, RateLimitConfig fallback) {
        int windowSeconds = parsePositiveInt(values.get(FIELD_WINDOW_SECONDS), fallback.windowSeconds());
        int maxRequests = parsePositiveInt(values.get(FIELD_MAX_REQUESTS), fallback.maxRequests());
        boolean enabled = values.containsKey(FIELD_ENABLED)
                ? Boolean.parseBoolean(values.get(FIELD_ENABLED))
                : fallback.enabled();
        return new RateLimitConfig(windowSeconds, maxRequests, enabled);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
