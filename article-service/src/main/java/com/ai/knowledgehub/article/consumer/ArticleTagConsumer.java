package com.ai.knowledgehub.article.consumer;

import com.ai.knowledgehub.article.config.MqConfig;
import com.ai.knowledgehub.article.service.ArticleService;
import com.ai.knowledgehub.article.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 文章发布事件消费者 - 标签提取
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleTagConsumer {

    private final TagService tagService;
    private final ArticleService articleService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8085}")
    private String aiServiceUrl;

    @RabbitListener(queues = MqConfig.ARTICLE_TAG_QUEUE)
    public void handleArticlePublished(Map<String, Object> event) {
        try {
            Long articleId = ((Number) event.get("articleId")).longValue();
            String title = (String) event.get("title");
            String content = (String) event.get("content");

            log.info("收到文章发布事件，开始提取标签，文章ID: {}", articleId);

            // 调用AI服务提取标签
            List<String> tags = extractTags(title, content);

            if (tags != null && !tags.isEmpty()) {
                // 保存标签
                tagService.addTagsToArticle(articleId, tags);
                log.info("文章标签提取完成，文章ID: {}, 标签: {}", articleId, tags);
            } else {
                log.info("文章标签提取结果为空，文章ID: {}", articleId);
            }
        } catch (Exception e) {
            log.error("处理文章标签提取事件失败，错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 调用AI服务提取标签
     */
    @SuppressWarnings("unchecked")
    private List<String> extractTags(String title, String content) {
        try {
            String url = aiServiceUrl + "/api/ai/extract-tags";
            Map<String, Object> request = Map.of(
                    "title", title,
                    "content", content
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    return (List<String>) data;
                }
            }
            return List.of();
        } catch (RestClientException e) {
            log.warn("调用AI标签提取服务失败，使用默认标签，错误: {}", e.getMessage());
            // 返回一些默认标签
            return extractDefaultTags(title, content);
        }
    }

    /**
     * 简单的关键词提取作为降级方案
     */
    private List<String> extractDefaultTags(String title, String content) {
        StringBuilder text = new StringBuilder();
        if (title != null) text.append(title).append(" ");
        if (content != null) text.append(content);

        String[] keywords = {"AI", "人工智能", "机器学习", "深度学习", "Java", "Spring", "Redis", "MySQL", "微服务", "架构"};
        return List.of(keywords).stream()
                .filter(keyword -> text.toString().contains(keyword))
                .limit(5)
                .toList();
    }
}