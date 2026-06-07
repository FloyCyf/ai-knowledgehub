package com.ai.knowledgehub.article.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
            createArticleTable(connection);
            createCommentTable(connection);
            createArticleLikeTable(connection);
            log.info("Database tables created successfully");
        } catch (SQLException e) {
            log.error("Failed to create database tables", e);
        }
    }

    private void createArticleTable(Connection connection) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS article (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                author_id BIGINT NOT NULL,
                title VARCHAR(255) NOT NULL,
                content TEXT,
                summary VARCHAR(500),
                status VARCHAR(20) DEFAULT 'DRAFT',
                view_count INT DEFAULT 0,
                like_count INT DEFAULT 0,
                comment_count INT DEFAULT 0,
                deleted TINYINT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                published_at TIMESTAMP NULL
            )
            """;
        executeSql(connection, sql, "article");
    }

    private void createCommentTable(Connection connection) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS comment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                article_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                content TEXT NOT NULL,
                deleted TINYINT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
            )
            """;
        executeSql(connection, sql, "comment");
    }

    private void createArticleLikeTable(Connection connection) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS article_like (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                article_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_user_article (user_id, article_id),
                FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
            )
            """;
        executeSql(connection, sql, "article_like");
    }

    private void executeSql(Connection connection, String sql, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.info("Table '{}' created or already exists", tableName);
        }
    }
}