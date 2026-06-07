package com.ai.knowledgehub.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章点赞实体类
 */
@Data
@TableName("article_like")
public class ArticleLike {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 点赞用户 ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}