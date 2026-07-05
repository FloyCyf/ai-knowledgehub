package com.ai.knowledgehub.gateway.ratelimit;

/**
 * 文章详情动态限流配置。
 *
 * @param windowSeconds 固定窗口秒数
 * @param maxRequests   窗口内最大请求数
 * @param enabled       是否启用限流
 */
public record RateLimitConfig(int windowSeconds, int maxRequests, boolean enabled) {

    public RateLimitConfig {
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
    }
}
