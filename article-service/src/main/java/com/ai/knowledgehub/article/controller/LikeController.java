package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.service.LikeService;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞控制器
 * 处理文章点赞接口
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 点赞文章
     *
     * @param id     文章 ID
     * @param userId 用户 ID（从网关透传）
     * @return 成功响应
     */
    @PostMapping("/{id}/like")
    public ApiResponse<Void> likeArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        likeService.likeArticle(id, userId);
        return ApiResponse.success();
    }
}