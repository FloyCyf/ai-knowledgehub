package com.ai.knowledgehub.ai.vo;

import lombok.Data;

/**
 * 文章 AI 分析结果 VO（包含标签和合规检测）
 */
@Data
public class ArticleAnalysisVO {

    /**
     * AI 标签提取结果
     */
    private AiTagVO tag;

    /**
     * AI 合规检测结果
     */
    private AuditResultVO audit;
}
