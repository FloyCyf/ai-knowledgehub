package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAuthorizationWebFilterTest {

    private static final String SECRET = "ai-knowledgehub-default-secret-key-for-jwt-token-generation-2024-do-not-use-in-prod";

    private AdminAuthorizationWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminAuthorizationWebFilter(new ObjectMapper());
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
    }

    @Test
    @DisplayName("网关本地 /api/admin/** 普通用户返回 403")
    void adminLocalControllerWithUserRole_returnsForbidden() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/admin/rate-limit/article-detail")
                        .header(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER + token(1L, "alice", "USER"))
                        .build()
        );

        filter.filter(exchange, new CapturingChain()).block();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
    }

    @Test
    @DisplayName("网关本地 /api/admin/** 管理员放行并覆盖用户头")
    void adminLocalControllerWithAdminRole_allowsAndOverwritesHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/admin/rate-limit/article-detail")
                        .header(HttpHeaders.AUTHORIZATION, HeaderConstants.BEARER + token(9L, "root", "ADMIN"))
                        .header(HeaderConstants.X_USER_ID, "1")
                        .header(HeaderConstants.X_USER_ROLE, "USER")
                        .build()
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertTrue(chain.called);
        assertEquals("9", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_ID));
        assertEquals("ADMIN", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_ROLE));
        assertEquals("root", chain.exchange.getRequest().getHeaders().getFirst(HeaderConstants.X_USER_NAME));
    }

    @Test
    @DisplayName("非管理员路径不拦截")
    void nonAdminPath_allows() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/user/profile").build()
        );
        CapturingChain chain = new CapturingChain();

        filter.filter(exchange, chain).block();

        assertTrue(chain.called);
    }

    private String token(Long userId, String username, String role) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMillis(86_400_000L);
        properties.setIssuer("ai-knowledgehub");
        return JwtUtil.generateToken(userId, username, role, properties);
    }

    private static class CapturingChain implements WebFilterChain {
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
