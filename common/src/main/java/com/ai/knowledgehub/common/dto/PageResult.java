package com.ai.knowledgehub.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页响应结果
 * <p>
 * 与 {@code docs/api-spec.md} 中"分页参数规范"配套使用，
 * 各服务统一返回该对象，前端无需关心不同服务的分页字段命名。
 * </p>
 *
 * @param <T> 列表元素类型
 * @author AI KnowledgeHub Team
 */
@Data
@Schema(description = "统一分页响应结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> list;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Integer page;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "10")
    private Integer size;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private Integer totalPages;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list == null ? Collections.emptyList() : list;
        this.total = total == null ? 0L : total;
        this.page = page;
        this.size = size;
        this.totalPages = (this.size == null || this.size == 0)
                ? 0
                : (int) Math.ceil((double) this.total / this.size);
    }

    /**
     * 快速构造空分页
     */
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size);
    }

    /**
     * 将当前分页中的元素转换为另一种类型
     *
     * @param converter 转换函数
     * @param <R>       目标类型
     * @return 转换后的新分页结果
     */
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> mappedList = this.list.stream()
                .map(converter)
                .collect(Collectors.toList());
        return new PageResult<>(mappedList, this.total, this.page, this.size);
    }
}
