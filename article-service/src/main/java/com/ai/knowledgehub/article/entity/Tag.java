package com.ai.knowledgehub.article.entity;

import com.ai.knowledgehub.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签颜色（十六进制颜色码）
     */
    private String color;

    /**
     * 使用次数
     */
    private Integer usageCount;
}