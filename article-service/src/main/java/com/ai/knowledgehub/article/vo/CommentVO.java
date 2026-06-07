package com.ai.knowledgehub.article.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 VO
 * 用于返回评论列表
 */
@Data
public class CommentVO {

    /**
     * 评论 ID
     */
    private Long id;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}