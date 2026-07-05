package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.gateway.ratelimit.RateLimitConfig;
import com.ai.knowledgehub.gateway.ratelimit.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitGlobalFilterTest {

    @Test
    @DisplayName("文章详情第 21 次请求触发 429")
    void articleDetailOverLimit_returnsTooManyRequests() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        RateLimitService rateLimitService = mock(RateLimitService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        when(rateLimitService.getArticleDetailConfig())
                .thenReturn(Mono.just(new RateLimitConfig(10, 20, true)));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(21L));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        RateLimitGlobalFilter filter = new RateLimitGlobalFilter(redisTemplate, rateLimitService, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/articles/1")
                        .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 12345))
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(429, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }
}
