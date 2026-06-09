package com.ai.knowledgehub.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * AI 服务数据库初始化器
 * 启动时自动创建 article_ai_tag 和 article_audit_result 表
 */
@Slf4j
@Component
public class DatabaseInitializer implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createTables();
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection()) {
            createArticleAiTagTable(connection);
            createArticleAuditResultTable(connection);
            log.info("AI 服务数据库表创建成功");
        } catch (SQLException e) {
            log.error("创建数据库表失败", e);
        }
    }

    private void createArticleAiTagTable(Connection connection) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS article_ai_tag (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                article_id BIGINT NOT NULL,
                tags TEXT,
                model_name VARCHAR(100),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeSql(connection, sql, "article_ai_tag");
    }

    private void createArticleAuditResultTable(Connection connection) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS article_audit_result (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                article_id BIGINT NOT NULL,
                result VARCHAR(20) NOT NULL,
                reason VARCHAR(1000),
                model_name VARCHAR(100),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeSql(connection, sql, "article_audit_result");
    }

    private void executeSql(Connection connection, String sql, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.info("表 '{}' 创建成功或已存在", tableName);
        }
    }
}
