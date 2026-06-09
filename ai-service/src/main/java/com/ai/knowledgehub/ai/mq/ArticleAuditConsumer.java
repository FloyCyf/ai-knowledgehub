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
 * 文章合规检测消费者
 * 监听 article.audit.queue，文章发布后异步进行 AI 合规检测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleAuditConsumer {

    private final AiService aiService;

    /**
     * 消费文章发布事件 - 合规检测
     * Jackson2JsonMessageConverter 自动将 JSON 消息反序列化为 Map<String, Object>
     */
    @RabbitListener(queues = MqConfig.ARTICLE_AUDIT_QUEUE)
    public void onArticlePublished(@Payload Map<String, Object> event,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Long articleId = ((Number) event.get("articleId")).longValue();
            String title = (String) event.get("title");
            String content = (String) event.get("content");

            log.info("收到文章发布事件[合规检测], articleId: {}, title: {}", articleId, title);

            aiService.auditArticle(articleId, title, content);

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("合规检测消费成功, articleId: {}", articleId);
        } catch (Exception e) {
            log.error("合规检测消费失败", e);
            try {
                // NACK 不重入队列，避免无限重试
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("NACK 失败", ex);
            }
        }
    }
}
