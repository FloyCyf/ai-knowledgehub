package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.dto.CommentDTO;
import com.ai.knowledgehub.article.service.CommentService;
import com.ai.knowledgehub.article.vo.CommentVO;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 评论控制器
 * 处理评论的创建和查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 评论文章
     *
     * @param id         文章 ID
     * @param commentDTO 评论数据
     * @param userId     用户 ID（从网关透传）
     * @return 评论 ID
     */
    @PostMapping("/{id}/comments")
    public ApiResponse<Map<String, Object>> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentDTO commentDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        Long commentId = commentService.createComment(id, commentDTO, userId);
        return ApiResponse.success(Map.of("commentId", commentId));
    }

    /**
     * 获取文章评论列表
     *
     * @param id   文章 ID
     * @param page 页码
     * @param size 每页数量
     * @return 评论列表
     */
    @GetMapping("/{id}/comments")
    public ApiResponse<Map<String, Object>> getCommentList(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<CommentVO> commentPage = commentService.getCommentList(id, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", commentPage.getRecords());
        data.put("total", commentPage.getTotal());
        data.put("page", commentPage.getCurrent());
        data.put("size", commentPage.getSize());

        return ApiResponse.success(data);
    }
}