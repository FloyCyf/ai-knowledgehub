package com.ai.knowledgehub.article.consumer;

import com.ai.knowledgehub.article.config.MqConfig;
import com.ai.knowledgehub.article.entity.Article;
import com.ai.knowledgehub.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 文章发布事件消费者 - 内容审核
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "article.ai-consumer.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ArticleAuditConsumer {

    private final ArticleService articleService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8085}")
    private String aiServiceUrl;

    @RabbitListener(queues = MqConfig.ARTICLE_AUDIT_QUEUE)
    public void handleArticlePublished(Map<String, Object> event) {
        try {
            Long articleId = ((Number) event.get("articleId")).longValue();
            String title = (String) event.get("title");
            String content = (String) event.get("content");

            log.info("收到文章发布事件，开始审核内容，文章ID: {}", articleId);

            // 调用AI服务进行审核
            AuditResult result = auditContent(title, content);

            // 根据审核结果处理
            if ("REJECT".equals(result.getResult())) {
                // 审核不通过，修改文章状态
                handleReject(articleId, result.getReason());
            } else if ("REVIEW".equals(result.getResult())) {
                // 需要人工审核
                handleReview(articleId);
            } else {
                // 审核通过
                log.info("文章审核通过，文章ID: {}", articleId);
            }
        } catch (Exception e) {
            log.error("处理文章审核事件失败，错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 调用AI服务进行内容审核
     */
    private AuditResult auditContent(String title, String content) {
        try {
            String url = aiServiceUrl + "/api/ai/audit";
            Map<String, Object> request = Map.of(
                    "title", title,
                    "content", content
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    return new AuditResult(
                            (String) dataMap.get("result"),
                            (String) dataMap.get("reason")
                    );
                }
            }
        } catch (RestClientException e) {
            log.warn("调用AI审核服务失败，使用本地敏感词过滤，错误: {}", e.getMessage());
            // 使用本地敏感词过滤作为降级方案
            return localAudit(title, content);
        }
        // 默认通过
        return new AuditResult("PASS", "自动审核通过");
    }

    /**
     * 本地敏感词过滤
     */
    private AuditResult localAudit(String title, String content) {
        String[] sensitiveWords = {"敏感词1", "敏感词2", "敏感词3", "test敏感词"};
        
        StringBuilder text = new StringBuilder();
        if (title != null) text.append(title);
        if (content != null) text.append(content);
        
        for (String word : sensitiveWords) {
            if (text.toString().contains(word)) {
                return new AuditResult("REJECT", "包含敏感内容");
            }
        }
        return new AuditResult("PASS", "本地审核通过");
    }

    /**
     * 处理审核不通过
     */
    private void handleReject(Long articleId, String reason) {
        try {
            Article article = articleService.getArticleById(articleId);
            // 可以选择删除文章或改为草稿状态
            article.setStatus("REJECTED");
            // 这里简化处理，不实际修改数据库
            log.warn("文章审核不通过，文章ID: {}, 原因: {}", articleId, reason);
        } catch (Exception e) {
            log.error("处理审核不通过失败，文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 处理需要人工审核
     */
    private void handleReview(Long articleId) {
        try {
            Article article = articleService.getArticleById(articleId);
            // 改为待审核状态
            article.setStatus("PENDING_REVIEW");
            // 这里简化处理，不实际修改数据库
            log.info("文章需要人工审核，文章ID: {}", articleId);
        } catch (Exception e) {
            log.error("处理待审核状态失败，文章ID: {}, 错误: {}", articleId, e.getMessage());
        }
    }

    /**
     * 审核结果
     */
    private static class AuditResult {
        private final String result;
        private final String reason;

        public AuditResult(String result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public String getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }
}
