package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Protects gateway-local admin controllers.
 *
 * <p>Spring Cloud Gateway GlobalFilter only applies to routed gateway traffic.
 * Gateway-local controllers, such as rate-limit admin APIs, need a WebFilter.</p>
 */
@Component
public class AdminAuthorizationWebFilter implements WebFilter, Ordered {

    private static final String ADMIN_PREFIX = "/api/admin/";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper;

    public AdminAuthorizationWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (HttpMethod.OPTIONS.equals(request.getMethod()) || !path.startsWith(ADMIN_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, ApiResponse.fail(401, "缺少认证 Token"));
        }

        try {
            Claims claims = validateToken(token);
            Long userId = JwtUtil.getUserId(claims);
            String username = JwtUtil.getUsername(claims);
            String role = JwtUtil.getRole(claims);

            if (userId == null || role == null || role.isBlank()) {
                return writeJson(exchange, HttpStatus.UNAUTHORIZED, ApiResponse.fail(401, "Token 缺少用户上下文"));
            }
            if (!HeaderConstants.ROLE_ADMIN.equals(role)) {
                return writeJson(exchange, HttpStatus.FORBIDDEN, ApiResponse.fail(403, "无权限访问"));
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(HeaderConstants.X_USER_ID);
                        headers.remove(HeaderConstants.X_USER_ROLE);
                        headers.remove(HeaderConstants.X_USER_NAME);
                        headers.set(HeaderConstants.X_USER_ID, String.valueOf(userId));
                        headers.set(HeaderConstants.X_USER_ROLE, role);
                        if (username != null && !username.isBlank()) {
                            headers.set(HeaderConstants.X_USER_NAME, username);
                        }
                    })
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, ApiResponse.fail(401, "Token 无效或已过期"));
        }
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith(HeaderConstants.BEARER)) {
            return bearerToken.substring(HeaderConstants.BEARER.length());
        }
        return null;
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
        return -200;
    }
}
