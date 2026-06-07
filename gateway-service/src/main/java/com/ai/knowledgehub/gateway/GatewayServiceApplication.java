package com.ai.knowledgehub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 网关服务启动类
 * <p>
 * 基于 Spring Cloud Gateway（WebFlux），不是 Spring MVC！
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ai.knowledgehub.gateway",
        "com.ai.knowledgehub.common"
})
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
