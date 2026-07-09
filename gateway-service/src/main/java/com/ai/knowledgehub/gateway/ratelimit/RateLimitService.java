package com.ai.knowledgehub.gateway.ratelimit;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Dynamic rate-limit configuration service backed by Nacos Config.
 */
@Service
public class RateLimitService {

    private final RateLimitProperties properties;
    private final NacosRateLimitConfigPublisher nacosPublisher;

    public RateLimitService(RateLimitProperties properties,
                            NacosRateLimitConfigPublisher nacosPublisher) {
        this.properties = properties;
        this.nacosPublisher = nacosPublisher;
    }

    public RateLimitConfig defaultConfig() {
        return new RateLimitConfig(
                properties.getArticleDetail().getWindowSeconds(),
                properties.getArticleDetail().getMaxRequests(),
                properties.isEnabled()
        );
    }

    public Mono<RateLimitConfig> getArticleDetailConfig() {
        return Mono.just(defaultConfig());
    }

    public Mono<RateLimitConfig> updateArticleDetailConfig(RateLimitConfig config) {
        return nacosPublisher.publishArticleDetailConfig(config);
    }
}
