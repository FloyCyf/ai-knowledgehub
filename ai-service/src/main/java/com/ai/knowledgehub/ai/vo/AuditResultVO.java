package com.ai.knowledgehub.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 合规检测结果 VO
 */
@Data
public class AuditResultVO {

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 检测结果: PASS/REVIEW/REJECT
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
