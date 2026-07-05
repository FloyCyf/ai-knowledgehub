package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.result.ApiResponse;
import com.ai.knowledgehub.gateway.ratelimit.RateLimitConfig;
import com.ai.knowledgehub.gateway.ratelimit.RateLimitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 文章详情固定窗口 IP + path 限流过滤器。
 * <p>
 * Redis 不可用时采用 fail-open 策略，记录日志后放行请求，保证课程演示链路稳定。
 * </p>
 */
@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitGlobalFilter(ReactiveStringRedisTemplate redisTemplate,
                                 RateLimitService rateLimitService,
                                 ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!isArticleDetailRequest(request)) {
            return chain.filter(exchange);
        }

        return rateLimitService.getArticleDetailConfig()
                .flatMap(config -> checkLimit(exchange, chain, config))
                .onErrorResume(e -> {
                    log.warn("限流检查失败，按 fail-open 策略放行: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> checkLimit(ServerWebExchange exchange, GatewayFilterChain chain, RateLimitConfig config) {
        if (!config.enabled()) {
            return chain.filter(exchange);
        }

        String key = buildKey(exchange.getRequest());
        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expire = count == 1
                            ? redisTemplate.expire(key, Duration.ofSeconds(config.windowSeconds()))
                            : Mono.just(true);
                    return expire.then(count > config.maxRequests()
                            ? writeJson(exchange, HttpStatus.TOO_MANY_REQUESTS,
                            ApiResponse.fail(429, "访问过于频繁，请稍后再试"))
                            : chain.filter(exchange));
                });
    }

    private boolean isArticleDetailRequest(ServerHttpRequest request) {
        return HttpMethod.GET.equals(request.getMethod())
                && request.getURI().getPath().matches("^/api/articles/[^/]+$");
    }

    private String buildKey(ServerHttpRequest request) {
        String ip = resolveClientIp(request);
        String path = request.getURI().getPath();
        return "rate_limit:" + ip + ":" + path;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, ApiResponse<?> response) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":500,\"message\":\"响应序列化失败\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
