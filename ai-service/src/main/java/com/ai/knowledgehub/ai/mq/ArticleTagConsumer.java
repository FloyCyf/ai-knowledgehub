package com.ai.knowledgehub.ai.mq;

import com.ai.knowledgehub.ai.config.MqConfig;
import com.ai.knowledgehub.ai.service.AiService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文章标签提取消费者
 * 监听 article.tag.queue，文章发布后异步提取 AI 标签
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleTagConsumer {

    private final AiService aiService;

    /**
     * 消费文章发布事件 - 标签提取
     * Jackson2JsonMessageConverter 自动将 JSON 消息反序列化为 Map<String, Object>
     */
    @RabbitListener(queues = MqConfig.ARTICLE_TAG_QUEUE)
    public void onArticlePublished(@Payload Map<String, Object> event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Long articleId = ((Number) event.get("articleId")).longValue();
            String title = (String) event.get("title");
            String content = (String) event.get("content");

            log.info("收到文章发布事件[标签提取], articleId: {}, title: {}", articleId, title);

            aiService.extractTags(articleId, title, content);

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("标签提取消费成功, articleId: {}", articleId);
        } catch (Exception e) {
            log.error("标签提取消费失败", e);
            try {
                // NACK 不重入队列，避免无限重试
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("NACK 失败", ex);
            }
        }
    }
}
