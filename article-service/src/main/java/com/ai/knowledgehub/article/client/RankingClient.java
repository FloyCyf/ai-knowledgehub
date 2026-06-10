package com.ai.knowledgehub.article.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * RankingService 客户端
 * 通过 HTTP 调用 ranking-service 的 API
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingClient {

    private final RestTemplate restTemplate;

    @Value("${ranking.service.url:http://localhost:8083}")
    private String rankingServiceUrl;

    /**
     * 通知 ranking-service 文章阅读
     */
    public void notifyView(Long articleId) {
        try {
            String url = rankingServiceUrl + "/api/ranking/articles/" + articleId + "/view";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            log.info("通知 ranking-service 阅读成功, 文章ID: {}, 响应: {}", articleId, response.getStatusCode());
        } catch (RestClientException e) {
            log.warn("通知 ranking-service 阅读失败, 文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 通知 ranking-service 文章点赞
     */
    public void notifyLike(Long articleId) {
        try {
            String url = rankingServiceUrl + "/api/ranking/articles/" + articleId + "/like";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            log.info("通知 ranking-service 点赞成功, 文章ID: {}, 响应: {}", articleId, response.getStatusCode());
        } catch (RestClientException e) {
            log.warn("通知 ranking-service 点赞失败, 文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 通知 ranking-service 文章评论
     */
    public void notifyComment(Long articleId) {
        try {
            String url = rankingServiceUrl + "/api/ranking/articles/" + articleId + "/comment";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            log.info("通知 ranking-service 评论成功, 文章ID: {}, 响应: {}", articleId, response.getStatusCode());
        } catch (RestClientException e) {
            log.warn("通知 ranking-service 评论失败, 文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 通知 ranking-service 文章发布
     */
    public void notifyPublish(Long articleId) {
        try {
            String url = rankingServiceUrl + "/api/ranking/articles/" + articleId + "/publish";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            log.info("通知 ranking-service 发布成功, 文章ID: {}, 响应: {}", articleId, response.getStatusCode());
        } catch (RestClientException e) {
            log.warn("通知 ranking-service 发布失败, 文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 获取热点文章排行 Top10
     *
     * @return 文章ID列表
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Long> getTopArticles() {
        try {
            String url = rankingServiceUrl + "/api/ranking/top10";
            ResponseEntity<java.util.Map> response = restTemplate.getForEntity(url, java.util.Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof java.util.List) {
                    return (java.util.List<Long>) data;
                }
            }
            return java.util.Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("获取热点文章排行失败, 错误: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}