package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway 统一 JWT 鉴权过滤器。
 * <p>
 * 放行注册、登录、健康检查和公开查询接口；其他接口统一解析 JWT，并覆盖客户端
 * 伪造的用户上下文请求头后再转发给下游服务。
 * </p>
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator",
            "/h2-console",
            "/doc.html",
            "/v3/api-docs"
    );

    private static final String ADMIN_PREFIX = "/api/admin/";
    private static final String TOKEN_BLACKLIST_KEY_PREFIX = "token:blacklist:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(ObjectMapper objectMapper, ReactiveStringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublicEndpoint(request)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            log.warn("请求路径 {} 缺少 Token", path);
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
            if (path.startsWith(ADMIN_PREFIX) && !HeaderConstants.ROLE_ADMIN.equals(role)) {
                return writeJson(exchange, HttpStatus.FORBIDDEN, ApiResponse.fail(403, "无权限访问"));
            }

            return isTokenBlacklisted(claims)
                    .flatMap(blacklisted -> {
                        if (blacklisted) {
                            return writeJson(exchange, HttpStatus.UNAUTHORIZED,
                                    ApiResponse.fail(401, "Token has been logged out"));
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
                    });
        } catch (Exception e) {
            log.warn("Token 验证失败: {}", e.getMessage());
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, ApiResponse.fail(401, "Token 无效或已过期"));
        }
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        if (PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        if (HttpMethod.POST.equals(method)
                && ("/api/user/register".equals(path) || "/api/user/login".equals(path))) {
            return true;
        }
        if (HttpMethod.GET.equals(method)
                && ("/api/articles/latest".equals(path)
                || path.matches("^/api/articles/[^/]+$")
                || "/api/ranking/top10".equals(path))) {
            return true;
        }
        return false;
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

    private Mono<Boolean> isTokenBlacklisted(Claims claims) {
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            jti = claims.getSubject();
        }
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }

        return redisTemplate.hasKey(TOKEN_BLACKLIST_KEY_PREFIX + jti)
                .map(Boolean.TRUE::equals)
                .onErrorResume(e -> {
                    log.warn("Token blacklist check failed, using fail-open policy: {}", e.getMessage());
                    return Mono.just(false);
                });
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
        return -100;
    }
}
