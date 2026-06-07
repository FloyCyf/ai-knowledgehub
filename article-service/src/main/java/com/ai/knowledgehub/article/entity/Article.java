package com.ai.knowledgehub.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体类
 */
@Data
@TableName("article")
public class Article {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章正文
     */
    private String content;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章状态：DRAFT / PUBLISHED
     */
    private String status;

    /**
     * 阅读数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
}