package com.ai.knowledgehub.gateway.filter;

import com.ai.knowledgehub.common.constant.HeaderConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.article-detail.window-seconds:10}")
    private int articleDetailWindowSeconds;

    @Value("${rate-limit.article-detail.max-requests:20}")
    private int articleDetailMaxRequests;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Map<String, WindowCounter> articleDetailCounters = new ConcurrentHashMap<>();

    public JwtAuthenticationFilter(ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    private static final List<String> WHITE_LIST = List.of(
            "/api/user/register",
            "/api/user/login",
            "/api/articles/latest",
            "/api/ranking/top10",
            "/api/ai",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromRequest(exchange.getRequest());
        if (token == null) {
            log.warn("请求路径 {} 缺少 Token", path);
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, 401, "缺少认证 Token");
        }

        Claims claims;
        try {
            claims = validateToken(token);
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, 2007, "Token 无效或已过期");
        }

        return isBlacklisted(claims)
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        return writeJson(exchange, HttpStatus.UNAUTHORIZED, 401, "Token 已注销");
                    }
                    return handleAuthorizedRequest(exchange, chain, path, claims);
                });
    }

    private Mono<Void> handleAuthorizedRequest(ServerWebExchange exchange,
                                               GatewayFilterChain chain,
                                               String path,
                                               Claims claims) {
        String role = claims.get("role", String.class);
        if (path.startsWith("/api/admin/") && !HeaderConstants.ROLE_ADMIN.equals(role)) {
            return writeJson(exchange, HttpStatus.FORBIDDEN, 403, "管理员权限不足");
        }

        if (isRateLimitConfigPath(path)) {
            return handleRateLimitConfig(exchange);
        }

        if (rateLimitEnabled && isArticleDetailRequest(exchange.getRequest())) {
            String ip = clientIp(exchange.getRequest());
            if (!allowArticleDetailRequest(ip)) {
                return writeJson(exchange, HttpStatus.TOO_MANY_REQUESTS, 429, "请求过于频繁，请稍后再试");
            }
        }

        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(HeaderConstants.X_USER_ID, String.valueOf(extractUserId(claims)))
                .header(HeaderConstants.X_USER_ROLE, role == null ? "" : role)
                .header(HeaderConstants.X_USER_NAME, nullToEmpty(claims.get("username", String.class)))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private Mono<Void> handleRateLimitConfig(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (HttpMethod.GET.equals(method)) {
            return writeJson(exchange, HttpStatus.OK, 200, (Object) currentRateLimitJson());
        }
        if (HttpMethod.PUT.equals(method) || HttpMethod.POST.equals(method)) {
            String windowSeconds = exchange.getRequest().getQueryParams().getFirst("windowSeconds");
            String maxRequests = exchange.getRequest().getQueryParams().getFirst("maxRequests");
            try {
                if (windowSeconds != null) {
                    articleDetailWindowSeconds = Math.max(1, Integer.parseInt(windowSeconds));
                }
                if (maxRequests != null) {
                    articleDetailMaxRequests = Math.max(1, Integer.parseInt(maxRequests));
                }
                articleDetailCounters.clear();
                return writeJson(exchange, HttpStatus.OK, 200, (Object) currentRateLimitJson());
            } catch (NumberFormatException e) {
                return writeJson(exchange, HttpStatus.BAD_REQUEST, 400, "限流参数必须是整数");
            }
        }
        return writeJson(exchange, HttpStatus.METHOD_NOT_ALLOWED, 405, "请求方法不允许");
    }

    private String currentRateLimitJson() {
        return String.format("{\"enabled\":%s,\"windowSeconds\":%d,\"maxRequests\":%d}",
                rateLimitEnabled, articleDetailWindowSeconds, articleDetailMaxRequests);
    }

    private Mono<Boolean> isBlacklisted(Claims claims) {
        if (redisTemplate == null) {
            return Mono.just(false);
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            jti = claims.getSubject();
        }
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti)
                .onErrorResume(e -> {
                    log.warn("Redis 不可用，跳过 Token 黑名单校验: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    private boolean allowArticleDetailRequest(String ip) {
        long now = System.currentTimeMillis();
        WindowCounter counter = articleDetailCounters.computeIfAbsent(ip, key -> new WindowCounter(now));
        synchronized (counter) {
            long windowMillis = articleDetailWindowSeconds * 1000L;
            if (now - counter.windowStartMillis >= windowMillis) {
                counter.windowStartMillis = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count <= articleDetailMaxRequests;
        }
    }

    private boolean isArticleDetailRequest(ServerHttpRequest request) {
        if (!HttpMethod.GET.equals(request.getMethod())) {
            return false;
        }
        String path = request.getURI().getPath();
        return path.matches("^/api/articles/\\d+$");
    }

    private boolean isRateLimitConfigPath(String path) {
        return "/api/admin/rate-limit/article-detail".equals(path);
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HeaderConstants.AUTHORIZATION);
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

    private Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, int code, String message) {
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d}",
                code, escaped, System.currentTimeMillis());
        return writeRawJson(exchange, status, body);
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, int code, Object dataJson) {
        String body = String.format("{\"code\":%d,\"message\":\"操作成功\",\"data\":%s,\"timestamp\":%d}",
                code, dataJson, System.currentTimeMillis());
        return writeRawJson(exchange, status, body);
    }

    private Mono<Void> writeRawJson(ServerWebExchange exchange, HttpStatus status, String body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static class WindowCounter {
        private long windowStartMillis;
        private int count;

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
