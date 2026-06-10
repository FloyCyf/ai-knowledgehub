package com.ai.knowledgehub.article.controller;

import com.ai.knowledgehub.article.dto.TagDTO;
import com.ai.knowledgehub.article.service.TagService;
import com.ai.knowledgehub.article.vo.TagVO;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 创建标签（管理员）
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createTag(
            @Valid @RequestBody TagDTO tagDTO,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        // 只有管理员可以创建标签
        if (!"ADMIN".equals(userRole)) {
            throw AuthException.forbidden();
        }

        Long tagId = tagService.createTag(tagDTO);
        return ApiResponse.success(Map.of("tagId", tagId));
    }

    /**
     * 更新标签（管理员）
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO tagDTO,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw AuthException.forbidden();
        }

        tagService.updateTag(id, tagDTO);
        return ApiResponse.success();
    }

    /**
     * 删除标签（管理员）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw AuthException.forbidden();
        }

        tagService.deleteTag(id);
        return ApiResponse.success();
    }

    /**
     * 分页获取标签列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getTagList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<TagVO> tagPage = tagService.getTagList(page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", tagPage.getRecords());
        data.put("total", tagPage.getTotal());
        data.put("page", tagPage.getCurrent());
        data.put("size", tagPage.getSize());

        return ApiResponse.success(data);
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    public ApiResponse<TagVO> getTagById(@PathVariable Long id) {
        TagVO tag = tagService.getTagById(id);
        return ApiResponse.success(tag);
    }

    /**
     * 获取文章的标签列表
     */
    @GetMapping("/article/{articleId}")
    public ApiResponse<List<TagVO>> getArticleTags(@PathVariable Long articleId) {
        List<TagVO> tags = tagService.getArticleTags(articleId);
        return ApiResponse.success(tags);
    }
}