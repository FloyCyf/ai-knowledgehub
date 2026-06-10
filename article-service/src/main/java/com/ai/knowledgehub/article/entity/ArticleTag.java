package com.ai.knowledgehub.article.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章标签关联表
 */
@Data
@TableName("article_tag")
public class ArticleTag {

    /**
     * ID
     */
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}