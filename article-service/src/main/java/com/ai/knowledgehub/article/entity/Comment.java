package com.ai.knowledgehub.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
@TableName("comment")
public class Comment {

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
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}