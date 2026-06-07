package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.dto.ArticleDTO;
import com.ai.knowledgehub.article.service.ArticleService;
import com.ai.knowledgehub.article.vo.ArticleVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章控制器
 * 处理文章的创建、修改、发布、删除、列表、详情等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 创建文章草稿
     *
     * @param articleDTO 文章数据
     * @param userId     用户 ID（从网关透传）
     * @return 文章 ID
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> createDraft(
            @Valid @RequestBody ArticleDTO articleDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        Long articleId = articleService.createDraft(articleDTO, userId);
        return ResponseEntity.ok(successResponse(Map.of("articleId", articleId)));
    }

    /**
     * 修改文章
     *
     * @param id         文章 ID
     * @param articleDTO 文章数据
     * @param userId     用户 ID（从网关透传）
     * @param userRole   用户角色（从网关透传）
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleDTO articleDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        try {
            articleService.updateArticle(id, articleDTO, userId, userRole);
            return ResponseEntity.ok(successResponse(null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("无权限")) {
                return ResponseEntity.status(403).body(errorResponse(403, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        }
    }

    /**
     * 发布文章
     *
     * @param id       文章 ID
     * @param userId   用户 ID（从网关透传）
     * @param userRole 用户角色（从网关透传）
     * @return 成功响应
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publishArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        try {
            articleService.publishArticle(id, userId, userRole);
            return ResponseEntity.ok(successResponse(null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("无权限")) {
                return ResponseEntity.status(403).body(errorResponse(403, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        }
    }

    /**
     * 删除文章（逻辑删除）
     *
     * @param id       文章 ID
     * @param userId   用户 ID（从网关透传）
     * @param userRole 用户角色（从网关透传）
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        try {
            articleService.deleteArticle(id, userId, userRole);
            return ResponseEntity.ok(successResponse(null));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("无权限")) {
                return ResponseEntity.status(403).body(errorResponse(403, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        }
    }

    /**
     * 分页获取最新文章列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 文章列表
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<ArticleVO> articlePage = articleService.getLatestArticles(page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", articlePage.getRecords());
        data.put("total", articlePage.getTotal());
        data.put("page", articlePage.getCurrent());
        data.put("size", articlePage.getSize());

        return ResponseEntity.ok(successResponse(data));
    }

    /**
     * 获取文章详情
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getArticleDetail(@PathVariable Long id) {
        try {
            ArticleVO article = articleService.getArticleDetail(id);
            return ResponseEntity.ok(successResponse(article));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(errorResponse(404, e.getMessage()));
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