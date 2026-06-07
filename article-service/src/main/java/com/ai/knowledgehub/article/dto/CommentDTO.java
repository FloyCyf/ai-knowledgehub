package com.ai.knowledgehub.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论 DTO
 * 用于评论文章的请求参数
 */
@Data
public class CommentDTO {

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;
}