package com.ai.knowledgehub.article;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文章服务启动类
 * 负责文章内容管理、评论和点赞功能
 */
@SpringBootApplication
@MapperScan("com.ai.knowledgehub.article.mapper")
public class ArticleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleApplication.class, args);
    }
}