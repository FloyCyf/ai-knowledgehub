package com.ai.knowledgehub.ranking.controller;

import com.ai.knowledgehub.common.result.ApiResponse;
import com.ai.knowledgehub.ranking.service.RankingService;
import com.ai.knowledgehub.ranking.vo.HotArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热榜控制器
 * <p>
 * 提供热榜相关的 REST API 接口，返回结构统一使用 common 模块的 {@link ApiResponse}。
 * 内部数据源支持两种模式：
 * <ul>
 *     <li>ranking.use-redis = true：使用 Redis ZSET（article:hot:ranking）</li>
 *     <li>ranking.use-redis = false：使用本地内存 Map（仅开发期使用）</li>
 * </ul>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 阅读文章 - 热度 +1
     *
     * @param id 文章 ID
     * @return 操作结果，含当前热度
     */
    @PostMapping("/articles/{id}/view")
    public ApiResponse<Map<String, Object>> incrementView(@PathVariable Long id) {
        log.info("文章阅读热度增加请求 - 文章ID: {}", id);

        rankingService.incrementViewScore(id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", id);
        data.put("action", "view");
        data.put("increment", 1);
        data.put("currentScore", rankingService.getArticleScore(id));

        return ApiResponse.success(data);
    }

    /**
     * 点赞文章 - 热度 +5
     *
     * @param id 文章 ID
     * @return 操作结果，含当前热度
     */
    @PostMapping("/articles/{id}/like")
    public ApiResponse<Map<String, Object>> incrementLike(@PathVariable Long id) {
        log.info("文章点赞热度增加请求 - 文章ID: {}", id);

        rankingService.incrementLikeScore(id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", id);
        data.put("action", "like");
        data.put("increment", 5);
        data.put("currentScore", rankingService.getArticleScore(id));

        return ApiResponse.success(data);
    }

    /**
     * 评论文章 - 热度 +3
     *
     * @param id 文章 ID
     * @return 操作结果，含当前热度
     */
    @PostMapping("/articles/{id}/comment")
    public ApiResponse<Map<String, Object>> incrementComment(@PathVariable Long id) {
        log.info("文章评论热度增加请求 - 文章ID: {}", id);

        rankingService.incrementCommentScore(id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", id);
        data.put("action", "comment");
        data.put("increment", 3);
        data.put("currentScore", rankingService.getArticleScore(id));

        return ApiResponse.success(data);
    }

    /**
     * 发布文章 - 热度 +2
     *
     * @param id 文章 ID
     * @return 操作结果，含当前热度
     */
    @PostMapping("/articles/{id}/publish")
    public ApiResponse<Map<String, Object>> incrementPublish(@PathVariable Long id) {
        log.info("文章发布热度增加请求 - 文章ID: {}", id);

        rankingService.incrementPublishScore(id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", id);
        data.put("action", "publish");
        data.put("increment", 2);
        data.put("currentScore", rankingService.getArticleScore(id));

        return ApiResponse.success(data);
    }

    /**
     * 获取热榜 Top10
     *
     * @return Top10 热榜文章列表（按热度降序）
     */
    @DeleteMapping("/articles/{id}")
    public ApiResponse<Map<String, Object>> removeArticle(@PathVariable Long id) {
        log.info("Remove article from ranking - articleId: {}", id);

        rankingService.removeArticle(id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", id);
        data.put("action", "remove");

        return ApiResponse.success(data);
    }

    @GetMapping("/top10")
    public ApiResponse<Map<String, Object>> getTop10() {
        log.info("获取热榜 Top10 请求");

        List<HotArticleVO> top10 = rankingService.getTop10();

        Map<String, Object> data = new HashMap<>();
        data.put("total", top10.size());
        data.put("articles", top10);

        return ApiResponse.success(data);
    }
}
