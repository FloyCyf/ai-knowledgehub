package com.ai.knowledgehub.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 标签提取结果 VO
 */
@Data
public class AiTagVO {

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * AI 提取的标签列表
     */
    private List<String> tags;

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
