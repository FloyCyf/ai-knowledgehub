package com.ai.knowledgehub.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes the local H2 schema used by the dev profile.
 */
@Slf4j
@Component
@Profile("dev")
@Order(1)
public class UserDatabaseInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin_demo";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    @Autowired
    private DataSource dataSource;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            createUserTable(conn);
            createDefaultAdmin(conn);
            log.info("User table initialized");
        } catch (SQLException e) {
            log.error("User table initialization failed", e);
        }
    }

    private void createUserTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `sys_user` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'user id',
                    `username` VARCHAR(50) NOT NULL COMMENT 'username',
                    `password_hash` VARCHAR(255) NOT NULL COMMENT 'bcrypt password hash',
                    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT 'user role',
                    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT 'user status',
                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
                    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
                    `create_by` BIGINT DEFAULT NULL COMMENT 'created by',
                    `update_by` BIGINT DEFAULT NULL COMMENT 'updated by',
                    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'logical delete flag',
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_username` (`username`),
                    KEY `idx_role` (`role`),
                    KEY `idx_status` (`status`)
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("Table 'sys_user' created or already exists");
        }
    }

    private void createDefaultAdmin(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0";
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, DEFAULT_ADMIN_USERNAME);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getLong(1) > 0) {
                    resetDefaultAdmin(conn);
                    return;
                }
            }
        }

        String insertSql = """
                INSERT INTO sys_user (username, password_hash, role, status, create_by, update_by)
                VALUES (?, ?, 'ADMIN', 'ENABLED', 0, 0)
                """;
        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setString(1, DEFAULT_ADMIN_USERNAME);
            insert.setString(2, passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            insert.executeUpdate();
            log.info("Default dev admin account created: {}", DEFAULT_ADMIN_USERNAME);
        }
    }

    private void resetDefaultAdmin(Connection conn) throws SQLException {
        String updateSql = """
                UPDATE sys_user
                SET password_hash = ?, role = 'ADMIN', status = 'ENABLED', deleted = 0
                WHERE username = ?
                """;
        try (PreparedStatement update = conn.prepareStatement(updateSql)) {
            update.setString(1, passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            update.setString(2, DEFAULT_ADMIN_USERNAME);
            update.executeUpdate();
            log.info("Default dev admin account reset: {}", DEFAULT_ADMIN_USERNAME);
        }
    }
}
