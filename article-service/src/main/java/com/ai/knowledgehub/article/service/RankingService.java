package com.ai.knowledgehub.article.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RankingService {

    private static final String HOT_RANKING_KEY = "article:hot:ranking";
    private static final ConcurrentSkipListMap<Long, Double> inMemoryRanking = new ConcurrentSkipListMap<>();
    private static final ConcurrentHashMap<Long, Double> inMemoryScores = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void increaseScore(Long articleId, int increment) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForZSet().incrementScore(HOT_RANKING_KEY, articleId, increment);
            } else {
                Double current = inMemoryScores.getOrDefault(articleId, 0.0);
                double newScore = current + increment;
                inMemoryScores.put(articleId, newScore);
                inMemoryRanking.put(articleId, newScore);
            }
        } catch (Exception e) {
            Double current = inMemoryScores.getOrDefault(articleId, 0.0);
            double newScore = current + increment;
            inMemoryScores.put(articleId, newScore);
            inMemoryRanking.put(articleId, newScore);
        }
    }

    public Set<Object> getTop10() {
        try {
            if (redisTemplate != null) {
                return redisTemplate.opsForZSet().reverseRange(HOT_RANKING_KEY, 0, 9);
            } else {
                return inMemoryRanking.keySet().stream().limit(10).map(id -> (Object) id).collect(java.util.stream.Collectors.toSet());
            }
        } catch (Exception e) {
            return inMemoryRanking.keySet().stream().limit(10).map(id -> (Object) id).collect(java.util.stream.Collectors.toSet());
        }
    }
}