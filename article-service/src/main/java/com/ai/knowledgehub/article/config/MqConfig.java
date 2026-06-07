package com.ai.knowledgehub.article.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}