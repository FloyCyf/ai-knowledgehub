package com.ai.knowledgehub.ranking.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热榜文章 VO
 * 
 * 用于返回热榜 Top10 中的文章信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotArticleVO {

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 热度分数
     */
    private Double hotScore;

}