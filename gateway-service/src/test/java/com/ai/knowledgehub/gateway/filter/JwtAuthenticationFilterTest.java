package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "ai-knowledgehub-default-secret-key-for-jwt-token-generation-2024-do-not-use-in-prod";

    private JwtAuthenticationFilter filter;
    private ReactiveStringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        filter = new JwtAuthenticationFilter(new ObjectMapper(), redisTemplate);
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    @Test
    @DisplayName("POST /api/user/login 不带 token 直接放行")
    void loginWithoutToken_isWhitelisted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/user/login").build()
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertTrue(chain.called);
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("普通用户访问 /api/admin/** 返回 403")
    void adminEndpointWithUserRole_returnsForbidden() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/admin/rate-limit/article-detail")
                        .header(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER + token(1L, "alice", "USER"))
                        .build()
        );

        filter.filter(exchange, new CapturingChain()).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
    }

    @Test
    @DisplayName("网关覆盖客户端伪造的 X-User-* 请求头")
    void validToken_overwritesSpoofedUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/articles/draft")
                        .header(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER + token(9L, "bob", "ADMIN"))
                        .header(HeaderConstants.X_USER_ID, "666")
                        .header(HeaderConstants.X_USER_ROLE, "USER")
                        .build()
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertEquals("9", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_ID));
        assertEquals("ADMIN", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_ROLE));
        assertEquals("bob", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_NAME));
    }

    @Test
    @DisplayName("Token 已加入 Redis 黑名单时返回 401")
    void blacklistedToken_returnsUnauthorized() {
        String token = token(10L, "logout_user", "USER");
        String jti = JwtUtil.parseToken(token, jwtProperties()).getId();
        when(redisTemplate.hasKey("token:blacklist:" + jti)).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER + token)
                        .build()
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertFalse(chain.called);
    }

    private String token(Long userId, String username, String role) {
        return JwtUtil.generateToken(userId, username, role, jwtProperties());
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMillis(86_400_000L);
        properties.setIssuer("ai-knowledgehub");
        return properties;
    }

    private static class CapturingChain implements GatewayFilterChain {
        private boolean called;
        private ServerWebExchange exchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.called = true;
            this.exchange = exchange;
            return Mono.empty();
        }
    }
}
