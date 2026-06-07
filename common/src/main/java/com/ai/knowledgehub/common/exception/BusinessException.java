package com.ai.knowledgehub.common.exception;

import com.ai.knowledgehub.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于业务逻辑中抛出的异常，可携带错误码和错误消息
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造业务异常（使用错误码枚举）
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造业务异常（使用错误码枚举和自定义消息）
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（使用错误码枚举和原因）
     *
     * @param resultCode 错误码枚举
     * @param cause      异常原因
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造业务异常（使用错误码枚举、自定义消息和原因）
     *
     * @param resultCode 错误码枚举
     * @param message   自定义错误消息
     * @param cause     异常原因
     */
    public BusinessException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 快速创建业务异常
     *
     * @param resultCode 错误码枚举
     * @return 业务异常
     */
    public static BusinessException of(ResultCode resultCode) {
        return new BusinessException(resultCode);
    }

    /**
     * 快速创建业务异常（带自定义消息）
     *
     * @param resultCode 错误码枚举
     * @param message   自定义错误消息
     * @return 业务异常
     */
    public static BusinessException of(ResultCode resultCode, String message) {
        return new BusinessException(resultCode, message);
    }

    /**
     * 快速创建业务异常（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 业务异常
     */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }
}