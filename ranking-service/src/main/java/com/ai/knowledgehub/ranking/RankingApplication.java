package com.ai.knowledgehub.ranking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 热榜服务启动类
 * 
 * 负责文章热度维护和 Top10 查询
 * 使用 Redis ZSET 实现实时热榜功能
 */
@SpringBootApplication
public class RankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RankingApplication.class, args);
    }

}