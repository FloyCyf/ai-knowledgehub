package com.ai.knowledgehub.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 AI 合规检测结果实体
 * 不继承 BaseEntity（该表无 updateTime/deleted 字段）
 */
@Data
@TableName("article_audit_result")
public class ArticleAuditResult {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 检测结果: PASS-通过, REVIEW-需人工审核, REJECT-拒绝
     */
    private String result;

    /**
     * 检测结果原因说明
     */
    private String reason;

    /**
     * 使用的 AI 模型名称
     */
    private String modelName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
