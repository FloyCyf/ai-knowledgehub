package com.ai.knowledgehub.article.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 VO
 * 用于返回文章列表和详情
 */
@Data
public class ArticleVO {

    /**
     * 文章 ID
     */
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
     * 文章正文（列表时不返回）
     */
    private String content;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章状态
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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
}