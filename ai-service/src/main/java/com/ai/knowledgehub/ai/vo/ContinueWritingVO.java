package com.ai.knowledgehub.ai.vo;

import lombok.Data;

/**
 * AI 续写响应 VO
 */
@Data
public class ContinueWritingVO {

    /**
     * AI 续写结果全文
     */
    private String content;

    /**
     * 使用的 AI 模型名称
     */
    private String modelName;
}
