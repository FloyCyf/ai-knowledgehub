-- H2 数据库初始化脚本

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `author_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `summary` VARCHAR(500),
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    `view_count` INT NOT NULL DEFAULT 0,
    `like_count` INT NOT NULL DEFAULT 0,
    `comment_count` INT NOT NULL DEFAULT 0,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `published_at` TIMESTAMP
);

-- 3. 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(1000) NOT NULL,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 文章点赞表
CREATE TABLE IF NOT EXISTS `article_like` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`)
);

-- 5. 文章AI标签表
CREATE TABLE IF NOT EXISTS `article_ai_tag` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL,
    `tags` TEXT,
    `model_name` VARCHAR(100),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 文章AI合规检测表
CREATE TABLE IF NOT EXISTS `article_audit_result` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL,
    `result` VARCHAR(20) NOT NULL,
    `reason` VARCHAR(1000),
    `model_name` VARCHAR(100),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO `user` (`username`, `password_hash`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2f6pG', 'ADMIN', 'ENABLED'),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2f6pG', 'USER', 'ENABLED');

INSERT INTO `article` (`author_id`, `title`, `content`, `summary`, `status`, `view_count`, `like_count`, `comment_count`, `deleted`, `published_at`) VALUES
(1, 'Redis ZSET 实现热榜', 'Redis ZSET 是一个有序集合，非常适合实现排行榜功能...', '本文介绍如何使用 Redis ZSET 实现实时热榜', 'PUBLISHED', 100, 20, 5, 0, CURRENT_TIMESTAMP),
(1, 'Spring Boot 入门教程', 'Spring Boot 是一个快速开发框架...', '从零开始学习 Spring Boot', 'PUBLISHED', 250, 45, 12, 0, CURRENT_TIMESTAMP);

INSERT INTO `comment` (`article_id`, `user_id`, `content`, `deleted`, `created_at`) VALUES
(1, 2, '这篇文章写得很好，学到了很多！', 0, CURRENT_TIMESTAMP),
(1, 1, '感谢支持！', 0, CURRENT_TIMESTAMP),
(2, 2, '入门教程很详细，适合新手', 0, CURRENT_TIMESTAMP);

INSERT INTO `article_like` (`article_id`, `user_id`, `created_at`) VALUES
(1, 2, CURRENT_TIMESTAMP),
(2, 2, CURRENT_TIMESTAMP);
