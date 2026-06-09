package com.ai.knowledgehub.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 AI 标签实体
 * 不继承 BaseEntity（该表无 updateTime/deleted 字段）
 */
@Data
@TableName("article_ai_tag")
public class ArticleAiTag {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * AI 生成的标签（JSON 数组字符串格式，如 ["Redis","缓存","NoSQL"]）
     */
    private String tags;

    /**
     * 使用的 AI 模型名称
     */
    private String modelName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
