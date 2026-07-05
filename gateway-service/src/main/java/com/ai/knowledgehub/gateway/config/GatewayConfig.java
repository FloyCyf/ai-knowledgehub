package com.ai.knowledgehub.gateway.config;

import com.ai.knowledgehub.gateway.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class GatewayConfig {
}
