package com.ai.knowledgehub.article.vo;

import lombok.Data;

/**
 * 标签VO
 */
@Data
public class TagVO {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色
     */
    private String color;

    /**
     * 使用次数
     */
    private Integer usageCount;
}