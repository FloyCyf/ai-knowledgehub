package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> likeArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        try {
            likeService.likeArticle(id, userId);
            return ResponseEntity.ok(successResponse(null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("已点赞")) {
                // 重复点赞返回 409 冲突
                return ResponseEntity.status(409).body(errorResponse(409, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        }
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> errorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}