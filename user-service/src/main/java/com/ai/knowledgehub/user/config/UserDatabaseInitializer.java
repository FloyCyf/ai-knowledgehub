package com.ai.knowledgehub.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 用户服务数据库初始化
 * <p>
 * 仅用于本地 H2 dev 环境。生产环境（MySQL）由 {@code docs/database-init.sql} 一次性建表。
 * </p>
 * <p>
 * 表结构与 {@code docs/database-design.md} 第 7~16 行 + {@code docs/database-init.sql} 第 19~32 行严格一致。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@Component
@Order(1)  // 优先于业务 Runner 执行
public class UserDatabaseInitializer implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            createUserTable(conn);
            log.info("User table 初始化完成");
        } catch (SQLException e) {
            log.error("User table 初始化失败", e);
        }
    }

    /**
     * 创建 user 表（与 docs/database-init.sql 一致）
     */
    private void createUserTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `user` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
                    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '用户角色',
                    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态',
                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
                    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
                    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_username` (`username`),
                    KEY `idx_role` (`role`),
                    KEY `idx_status` (`status`)
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("Table 'user' created or already exists");
        }
    }
}
