package com.ai.knowledgehub.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 续写请求 DTO
 */
@Data
public class ContinueWritingDTO {

    /**
     * 续写提示
     */
    @NotBlank(message = "续写提示不能为空")
    private String prompt;
}
