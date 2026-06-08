package com.ai.knowledgehub.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 声明与 article-service 相同的 Fanout Exchange、队列和绑定
 * 确保 ai-service 先于 article-service 启动时队列和交换机也能正确创建
 */
@Configuration
public class MqConfig {

    public static final String ARTICLE_PUBLISH_EXCHANGE = "article.publish.exchange";
    public static final String ARTICLE_TAG_QUEUE = "article.tag.queue";
    public static final String ARTICLE_AUDIT_QUEUE = "article.audit.queue";

    @Bean
    public FanoutExchange articlePublishExchange() {
        return new FanoutExchange(ARTICLE_PUBLISH_EXCHANGE, true, false);
    }

    @Bean("articleTagQueue")
    public Queue articleTagQueue() {
        return new Queue(ARTICLE_TAG_QUEUE, true);
    }

    @Bean("articleAuditQueue")
    public Queue articleAuditQueue() {
        return new Queue(ARTICLE_AUDIT_QUEUE, true);
    }

    @Bean
    public Binding tagBinding(FanoutExchange articlePublishExchange,
                              @Qualifier("articleTagQueue") Queue articleTagQueue) {
        return BindingBuilder.bind(articleTagQueue).to(articlePublishExchange);
    }

    @Bean
    public Binding auditBinding(FanoutExchange articlePublishExchange,
                                @Qualifier("articleAuditQueue") Queue articleAuditQueue) {
        return BindingBuilder.bind(articleAuditQueue).to(articlePublishExchange);
    }

    /**
     * 使用 JSON 序列化 MQ 消息，确保生产端和消费端格式一致
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
