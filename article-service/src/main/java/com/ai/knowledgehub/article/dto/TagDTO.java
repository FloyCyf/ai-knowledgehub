package com.ai.knowledgehub.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签DTO
 */
@Data
public class TagDTO {

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 50, message = "标签名称长度必须在1~50之间")
    private String name;

    /**
     * 标签颜色
     */
    private String color;
}