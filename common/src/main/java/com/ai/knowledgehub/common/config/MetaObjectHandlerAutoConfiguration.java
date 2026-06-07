package com.ai.knowledgehub.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MetaObjectHandler 自动注册
 * <p>
 * 避免 common 模块以 jar 形式被依赖时，{@code @Component} 注解无法被业务模块
 * 扫描到的问题。改为通过 Spring AutoConfiguration 注册。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Configuration
@ConditionalOnClass(MetaObjectHandler.class)
public class MetaObjectHandlerAutoConfiguration {

    /**
     * 注册默认的自动填充处理器
     * <p>
     * 业务模块可通过自定义同类型 Bean 覆盖。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MybatisMetaObjectHandler mybatisMetaObjectHandler() {
        return new MybatisMetaObjectHandler();
    }
}
