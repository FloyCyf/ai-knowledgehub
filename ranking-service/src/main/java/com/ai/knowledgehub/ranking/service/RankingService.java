package com.ai.knowledgehub.ranking.service;

import com.ai.knowledgehub.ranking.vo.HotArticleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 热榜服务
 * 
 * 支持两种模式：
 * 1. Redis ZSET 模式（use-redis: true）
 * 2. 内存 Map 模式（use-redis: false）
 * 
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

    @Value("${ranking.use-redis:false}")
    private boolean useRedis;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 内存模式下的热度存储
     */
    private final Map<Long, Double> memoryHotScore = new ConcurrentHashMap<>();

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
        if (useRedis && stringRedisTemplate != null) {
            return getTop10FromRedis();
        } else {
            return getTop10FromMemory();
        }
    }

    /**
     * 从 Redis 获取热榜 Top10
     */
    private List<HotArticleVO> getTop10FromRedis() {
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

        log.info("获取热榜 Top10（Redis 模式），共 {} 篇文章", result.size());
        return result;
    }

    /**
     * 从内存获取热榜 Top10
     */
    private List<HotArticleVO> getTop10FromMemory() {
        if (memoryHotScore.isEmpty()) {
            return Collections.emptyList();
        }

        List<HotArticleVO> result = memoryHotScore.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_N)
                .map(entry -> HotArticleVO.builder()
                        .articleId(entry.getKey())
                        .hotScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        log.info("获取热榜 Top10（内存模式），共 {} 篇文章", result.size());
        return result;
    }

    /**
     * 获取文章当前热度
     * 
     * @param articleId 文章 ID
     * @return 热度分数，不存在则返回 0
     */
    public Double getArticleScore(Long articleId) {
        if (useRedis && stringRedisTemplate != null) {
            Double score = stringRedisTemplate.opsForZSet().score(HOT_RANKING_KEY, articleId.toString());
            return score != null ? score : 0.0;
        } else {
            return memoryHotScore.getOrDefault(articleId, 0.0);
        }
    }

    /**
     * 通用热度增加方法
     * 
     * @param articleId 文章 ID
     * @param score     增加的分数
     * @param action    操作类型（用于日志）
     */
    private void incrementScore(Long articleId, double score, String action) {
        if (useRedis && stringRedisTemplate != null) {
            incrementScoreInRedis(articleId, score, action);
        } else {
            incrementScoreInMemory(articleId, score, action);
        }
    }

    /**
     * Redis 模式：增加热度
     */
    private void incrementScoreInRedis(Long articleId, double score, String action) {
        String articleIdStr = articleId.toString();
        Double newScore = stringRedisTemplate.opsForZSet()
                .incrementScore(HOT_RANKING_KEY, articleIdStr, score);
        log.info("文章 {} 热度更新（Redis）- 操作: {}, 增量: {}, 新分数: {}", 
                articleId, action, score, newScore);
    }

    /**
     * 内存模式：增加热度
     */
    private void incrementScoreInMemory(Long articleId, double score, String action) {
        Double currentScore = memoryHotScore.getOrDefault(articleId, 0.0);
        Double newScore = currentScore + score;
        memoryHotScore.put(articleId, newScore);
        log.info("文章 {} 热度更新（内存）- 操作: {}, 增量: {}, 新分数: {}", 
                articleId, action, score, newScore);
    }
}