package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.dto.CommentDTO;
import com.ai.knowledgehub.article.service.CommentService;
import com.ai.knowledgehub.article.vo.CommentVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentDTO commentDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 未登录校验
        if (userId == null) {
            return ResponseEntity.status(401).body(errorResponse(401, "请先登录"));
        }

        try {
            Long commentId = commentService.createComment(id, commentDTO, userId);
            return ResponseEntity.ok(successResponse(Map.of("commentId", commentId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(400, e.getMessage()));
        }
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
    public ResponseEntity<Map<String, Object>> getCommentList(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<CommentVO> commentPage = commentService.getCommentList(id, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", commentPage.getRecords());
        data.put("total", commentPage.getTotal());
        data.put("page", commentPage.getCurrent());
        data.put("size", commentPage.getSize());

        return ResponseEntity.ok(successResponse(data));
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> errorResponse(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}