package com.ai.knowledgehub.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章 DTO
 * 用于创建和修改文章的请求参数
 */
@Data
public class ArticleDTO {

    /**
     * 文章标题
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 文章正文
     */
    @NotBlank(message = "文章正文不能为空")
    private String content;

    /**
     * 文章摘要
     */
    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;
}