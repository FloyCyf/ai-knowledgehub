package com.ai.knowledgehub.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应结构
 * <p>
 * 所有接口返回的数据都使用此结构进行封装
 * </p>
 *
 * @param <T> 响应数据类型
 * @author AI KnowledgeHub Team
 */
@Data
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（有数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败响应（使用错误码枚举）
     *
     * @param resultCode 错误码枚举
     * @param <T>        数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(ResultCode resultCode) {
        return new ApiResponse<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 失败响应（使用错误码枚举和自定义消息）
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误消息
     * @param <T>        数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(ResultCode resultCode, String message) {
        return new ApiResponse<>(resultCode.getCode(), message, null);
    }

    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}