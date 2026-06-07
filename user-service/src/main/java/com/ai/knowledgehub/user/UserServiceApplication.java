package com.ai.knowledgehub.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 用户服务启动类
 * <p>
 * 扫描范围同时覆盖本服务（{@code com.ai.knowledgehub.user}）和
 * 公共模块（{@code com.ai.knowledgehub.common}），以便加载
 * {@code GlobalExceptionHandler} / {@code MetaObjectHandler} 等。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ai.knowledgehub.user",
        "com.ai.knowledgehub.common"
})
@MapperScan(basePackages = "com.ai.knowledgehub.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
