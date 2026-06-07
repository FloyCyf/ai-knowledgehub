-- ============================================
-- AI KnowledgeHub 数据库初始化脚本
-- 数据库: ai_knowledgehub
-- 创建日期: 2026-06-06
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_knowledgehub
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_knowledgehub;

-- ============================================
-- 1. 用户表 (user)
-- ============================================
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色: USER-普通用户, ADMIN-管理员',
    `status` ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'ENABLED' COMMENT '用户状态: ENABLED-启用, DISABLED-禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 文章表 (article)
-- ============================================
DROP TABLE IF EXISTS `article`;

CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content` LONGTEXT NOT NULL COMMENT '文章内容',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    `status` ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT' COMMENT '文章状态: DRAFT-草稿, PUBLISHED-已发布',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论次数',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_published_at` (`published_at`),
    KEY `idx_view_count` (`view_count`),
    KEY `idx_like_count` (`like_count`),
    CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ============================================
-- 3. 评论表 (comment)
-- ============================================
DROP TABLE IF EXISTS `comment`;

CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================
-- 4. 文章点赞表 (article_like)
-- ============================================
DROP TABLE IF EXISTS `article_like`;

CREATE TABLE `article_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`) COMMENT '防止同一用户对同一文章重复点赞',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_article_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- ============================================
-- 5. 文章AI标签表 (article_ai_tag)
-- ============================================
DROP TABLE IF EXISTS `article_ai_tag`;

CREATE TABLE `article_ai_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `tags` JSON DEFAULT NULL COMMENT 'AI生成的标签(JSON数组格式)',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT '使用的AI模型名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_model_name` (`model_name`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_article_ai_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章AI标签表';

-- ============================================
-- 6. 文章AI合规检测结果表 (article_audit_result)
-- ============================================
DROP TABLE IF EXISTS `article_audit_result`;

CREATE TABLE `article_audit_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '检测结果ID',
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `result` ENUM('PASS', 'REVIEW', 'REJECT') NOT NULL COMMENT '检测结果: PASS-通过, REVIEW-需人工审核, REJECT-拒绝',
    `reason` VARCHAR(500) DEFAULT NULL COMMENT '检测结果原因说明',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT '使用的AI模型名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_id` (`article_id`),
    KEY `idx_result` (`result`),
    KEY `idx_model_name` (`model_name`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_article_audit_result_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章AI合规检测结果表';

-- ============================================
-- 初始化管理员账户 (可选)
-- 默认密码: admin123 (请使用 BCrypt 加密)
-- ============================================
-- INSERT INTO `user` (`username`, `password_hash`, `role`, `status`)
-- VALUES ('admin', '$2a$10$...', 'ADMIN', 'ENABLED');