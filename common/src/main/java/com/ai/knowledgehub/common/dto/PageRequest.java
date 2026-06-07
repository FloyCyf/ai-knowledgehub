package com.ai.knowledgehub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一分页请求参数
 * <p>
 * 与 {@code docs/api-spec.md} 第 50~56 行约定的分页参数严格保持一致。
 * </p>
 * <ul>
 *     <li>{@code page} 页码，从 1 开始</li>
 *     <li>{@code size} 每页数量，默认 10</li>
 * </ul>
 *
 * @author AI KnowledgeHub Team
 */
@Data
@Schema(description = "统一分页请求参数")
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页数量
     */
    public static final int DEFAULT_SIZE = 10;

    /**
     * 最大每页数量（防止单次拉取过多）
     */
    public static final int MAX_SIZE = 100;

    /**
     * 页码，从 1 开始
     */
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer page = DEFAULT_PAGE;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "10", defaultValue = "10")
    @Min(value = 1, message = "每页数量必须大于等于 1")
    @Max(value = MAX_SIZE, message = "每页数量不能超过 " + MAX_SIZE)
    private Integer size = DEFAULT_SIZE;

    /**
     * 获取经过校正的页码（防止 null）
     */
    public int getEffectivePage() {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 获取经过校正的每页数量（防止 null 或过大）
     */
    public int getEffectiveSize() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 转换为 MyBatis-Plus 的 offset（用于手写分页 SQL）
     */
    public long offset() {
        return (long) (getEffectivePage() - 1) * getEffectiveSize();
    }
}
