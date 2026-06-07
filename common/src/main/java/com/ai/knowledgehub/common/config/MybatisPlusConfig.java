package com.ai.knowledgehub.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * MyBatis-Plus 通用配置
 * <p>
 * 提供分页插件、MetaObjectHandler 自动填充。依赖业务模块引入
 * {@code mybatis-plus-spring-boot3-starter} 后生效，使用
 * {@link ConditionalOnClass} 防止 common 模块单独启动失败。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Configuration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class MybatisPlusConfig {

    /**
     * 分页插件（支持 MySQL / H2 / PostgreSQL）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 自动根据 dialect 适配；明确指定 MySQL 兼容更稳定
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
