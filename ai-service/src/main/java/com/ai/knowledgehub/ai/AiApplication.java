package com.ai.knowledgehub.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 服务启动类
 * 负责 AI 续写、MQ 消费、标签提取和合规检测
 */
@SpringBootApplication
@MapperScan("com.ai.knowledgehub.ai.mapper")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
