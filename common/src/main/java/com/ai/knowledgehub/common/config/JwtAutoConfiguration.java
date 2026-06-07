package com.ai.knowledgehub.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 自动配置
 * <p>
 * 业务模块引用 common 后，启动时自动注册 {@link JwtProperties}，
 * 并把配置注入到 {@link JwtUtil} 静态方法中。
 * </p>
 *
 * <p>如需在业务模块中自定义密钥，可在自己的启动类上：</p>
 * <pre>
 *   &#64;SpringBootApplication
 *   &#64;EnableConfigurationProperties(JwtProperties.class)
 *   &#64;ConfigurationPropertiesScan
 * </pre>
 *
 * @author AI KnowledgeHub Team
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnProperty(prefix = "jwt", name = "secret")
public class JwtAutoConfiguration {

    /**
     * 应用启动时将 JwtProperties 注入 JwtUtil 静态上下文
     */
    @Bean
    public JwtInitializer jwtInitializer(JwtProperties properties) {
        properties.validate();
        JwtUtil.setDefaultProperties(properties);
        return new JwtInitializer();
    }

    /**
     * 占位 Bean，仅用于标识自动配置已执行
     */
    public static class JwtInitializer {
    }
}
