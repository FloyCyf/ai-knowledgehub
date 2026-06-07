package com.ai.knowledgehub.ranking.controller;

import com.ai.knowledgehub.ranking.service.RankingService;
import com.ai.knowledgehub.ranking.vo.HotArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热榜控制器
 * 
 * 提供热榜相关的 REST API 接口
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
     * @return 操作结果
     */
    @PostMapping("/articles/{id}/view")
    public ResponseEntity<Map<String, Object>> incrementView(@PathVariable Long id) {
        log.info("文章阅读热度增加请求 - 文章ID: {}", id);
        
        rankingService.incrementViewScore(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("action", "view");
        result.put("increment", 1);
        result.put("currentScore", rankingService.getArticleScore(id));
        
        return ResponseEntity.ok(success(result));
    }

    /**
     * 点赞文章 - 热度 +5
     * 
     * @param id 文章 ID
     * @return 操作结果
     */
    @PostMapping("/articles/{id}/like")
    public ResponseEntity<Map<String, Object>> incrementLike(@PathVariable Long id) {
        log.info("文章点赞热度增加请求 - 文章ID: {}", id);
        
        rankingService.incrementLikeScore(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("action", "like");
        result.put("increment", 5);
        result.put("currentScore", rankingService.getArticleScore(id));
        
        return ResponseEntity.ok(success(result));
    }

    /**
     * 评论文章 - 热度 +3
     * 
     * @param id 文章 ID
     * @return 操作结果
     */
    @PostMapping("/articles/{id}/comment")
    public ResponseEntity<Map<String, Object>> incrementComment(@PathVariable Long id) {
        log.info("文章评论热度增加请求 - 文章ID: {}", id);
        
        rankingService.incrementCommentScore(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("action", "comment");
        result.put("increment", 3);
        result.put("currentScore", rankingService.getArticleScore(id));
        
        return ResponseEntity.ok(success(result));
    }

    /**
     * 发布文章 - 热度 +2
     * 
     * @param id 文章 ID
     * @return 操作结果
     */
    @PostMapping("/articles/{id}/publish")
    public ResponseEntity<Map<String, Object>> incrementPublish(@PathVariable Long id) {
        log.info("文章发布热度增加请求 - 文章ID: {}", id);
        
        rankingService.incrementPublishScore(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("articleId", id);
        result.put("action", "publish");
        result.put("increment", 2);
        result.put("currentScore", rankingService.getArticleScore(id));
        
        return ResponseEntity.ok(success(result));
    }

    /**
     * 获取热榜 Top10
     * 
     * @return Top10 热榜文章列表
     */
    @GetMapping("/top10")
    public ResponseEntity<Map<String, Object>> getTop10() {
        log.info("获取热榜 Top10 请求");
        
        List<HotArticleVO> top10 = rankingService.getTop10();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", top10.size());
        result.put("articles", top10);
        
        return ResponseEntity.ok(success(result));
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

}