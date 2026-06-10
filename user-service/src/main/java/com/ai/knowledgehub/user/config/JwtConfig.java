package com.ai.knowledgehub.user.config;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public Object jwtInitializer(JwtProperties properties) {
        properties.validate();
        JwtUtil.setDefaultProperties(properties);
        return new Object();
    }
}