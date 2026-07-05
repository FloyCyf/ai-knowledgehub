package com.ai.knowledgehub.gateway.controller;

import com.ai.knowledgehub.common.result.ApiResponse;
import com.ai.knowledgehub.gateway.ratelimit.RateLimitConfig;
import com.ai.knowledgehub.gateway.ratelimit.RateLimitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Gateway 内部动态限流管理接口。
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
public class RateLimitAdminController {

    private final RateLimitService rateLimitService;

    public RateLimitAdminController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/article-detail")
    public Mono<ApiResponse<RateLimitConfig>> getArticleDetailConfig() {
        return rateLimitService.getArticleDetailConfig().map(ApiResponse::success);
    }

    @PutMapping("/article-detail")
    public Mono<ApiResponse<RateLimitConfig>> updateArticleDetailConfig(@RequestBody RateLimitConfig config) {
        return rateLimitService.updateArticleDetailConfig(config).map(ApiResponse::success);
    }
}
