package com.ai.knowledgehub.ranking.service;

import com.ai.knowledgehub.ranking.vo.HotArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 热榜服务
 * 
 * 使用 Redis ZSET 实现文章热度排行
 * Key: article:hot:ranking
 * 
 * 热度规则：
 * - 阅读文章：+1
 * - 发布文章：+2
 * - 评论文章：+3
 * - 点赞文章：+5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    /**
     * 热榜 ZSET 的 Redis Key
     */
    private static final String HOT_RANKING_KEY = "article:hot:ranking";

    /**
     * 阅读热度增量
     */
    private static final double VIEW_SCORE = 1.0;

    /**
     * 发布热度增量
     */
    private static final double PUBLISH_SCORE = 2.0;

    /**
     * 评论热度增量
     */
    private static final double COMMENT_SCORE = 3.0;

    /**
     * 点赞热度增量
     */
    private static final double LIKE_SCORE = 5.0;

    /**
     * Top10 数量
     */
    private static final long TOP_N = 10;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 增加文章阅读热度
     * 
     * @param articleId 文章 ID
     */
    public void incrementViewScore(Long articleId) {
        incrementScore(articleId, VIEW_SCORE, "阅读");
    }

    /**
     * 增加文章发布热度
     * 
     * @param articleId 文章 ID
     */
    public void incrementPublishScore(Long articleId) {
        incrementScore(articleId, PUBLISH_SCORE, "发布");
    }

    /**
     * 增加文章评论热度
     * 
     * @param articleId 文章 ID
     */
    public void incrementCommentScore(Long articleId) {
        incrementScore(articleId, COMMENT_SCORE, "评论");
    }

    /**
     * 增加文章点赞热度
     * 
     * @param articleId 文章 ID
     */
    public void incrementLikeScore(Long articleId) {
        incrementScore(articleId, LIKE_SCORE, "点赞");
    }

    /**
     * 获取热榜 Top10
     * 
     * @return 热榜文章列表，按热度降序排列
     */
    public List<HotArticleVO> getTop10() {
        // 使用 ZREVRANGE 获取 Top10（按分数降序）
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_RANKING_KEY, 0, TOP_N - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<HotArticleVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String articleIdStr = tuple.getValue();
            Double score = tuple.getScore();
            
            if (articleIdStr != null && score != null) {
                HotArticleVO vo = HotArticleVO.builder()
                        .articleId(Long.parseLong(articleIdStr))
                        .hotScore(score)
                        .build();
                result.add(vo);
            }
        }

        log.info("获取热榜 Top10 成功，共 {} 篇文章", result.size());
        return result;
    }

    /**
     * 获取文章当前热度
     * 
     * @param articleId 文章 ID
     * @return 热度分数，不存在则返回 0
     */
    public Double getArticleScore(Long articleId) {
        Double score = stringRedisTemplate.opsForZSet().score(HOT_RANKING_KEY, articleId.toString());
        return score != null ? score : 0.0;
    }

    /**
     * 通用热度增加方法
     * 
     * @param articleId 文章 ID
     * @param score     增加的分数
     * @param action    操作类型（用于日志）
     */
    private void incrementScore(Long articleId, double score, String action) {
        String articleIdStr = articleId.toString();
        
        // 使用 ZINCRBY 原子性增加分数
        Double newScore = stringRedisTemplate.opsForZSet()
                .incrementScore(HOT_RANKING_KEY, articleIdStr, score);
        
        log.info("文章 {} 热度更新 - 操作: {}, 增量: {}, 新分数: {}", 
                articleId, action, score, newScore);
    }

}