package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.dto.ArticleDTO;
import com.ai.knowledgehub.article.service.ArticleService;
import com.ai.knowledgehub.article.vo.ArticleVO;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    public ApiResponse<Map<String, Object>> createDraft(
            @Valid @RequestBody ArticleDTO articleDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        Long articleId = articleService.createDraft(articleDTO, userId);
        return ApiResponse.success(Map.of("articleId", articleId));
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
    public ApiResponse<Void> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleDTO articleDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        articleService.updateArticle(id, articleDTO, userId, userRole);
        return ApiResponse.success();
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
    public ApiResponse<Void> publishArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        articleService.publishArticle(id, userId, userRole);
        return ApiResponse.success();
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
    public ApiResponse<Void> deleteArticle(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        articleService.deleteArticle(id, userId, userRole);
        return ApiResponse.success();
    }

    /**
     * 分页获取最新文章列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 文章列表
     */
    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> getLatestArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<ArticleVO> articlePage = articleService.getLatestArticles(page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", articlePage.getRecords());
        data.put("total", articlePage.getTotal());
        data.put("page", articlePage.getCurrent());
        data.put("size", articlePage.getSize());

        return ApiResponse.success(data);
    }

    /**
     * 获取文章详情
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleVO> getArticleDetail(@PathVariable Long id) {     
        ArticleVO article = articleService.getArticleDetail(id);
        return ApiResponse.success(article);
    }

    /**
     * 获取热门文章列表（Top 10）
     *
     * @return 热门文章列表
     */
    @GetMapping("/hot")
    public ApiResponse<List<ArticleVO>> getHotArticles() {
        List<ArticleVO> hotArticles = articleService.getHotArticles();
        return ApiResponse.success(hotArticles);
    }
}