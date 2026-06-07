package com.ai.knowledgehub.common.exception;

import com.ai.knowledgehub.common.result.ApiResponse;
import com.ai.knowledgehub.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理各类异常，返回标准化的错误响应
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e      业务异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 - 请求路径: {}, 错误码: {}, 错误消息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     *
     * @param e      参数校验异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败 - 请求路径: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return ApiResponse.fail(ResultCode.PARAM_VALID_ERROR, errorMessage);
    }

    /**
     * 处理绑定异常
     *
     * @param e      绑定异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败 - 请求路径: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return ApiResponse.fail(ResultCode.PARAM_VALID_ERROR, errorMessage);
    }

    /**
     * 处理约束违反异常
     *
     * @param e      约束违反异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验失败 - 请求路径: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return ApiResponse.fail(ResultCode.PARAM_VALID_ERROR, errorMessage);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e      缺少请求参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少请求参数 - 请求路径: {}, 参数名: {}", request.getRequestURI(), e.getParameterName());
        return ApiResponse.fail(ResultCode.BAD_REQUEST, "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     *
     * @param e      参数类型不匹配异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配 - 请求路径: {}, 参数名: {}", request.getRequestURI(), e.getName());
        return ApiResponse.fail(ResultCode.BAD_REQUEST, "参数类型不匹配: " + e.getName());
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e      请求方法不支持异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 - 请求路径: {}, 请求方法: {}", request.getRequestURI(), e.getMethod());
        return ApiResponse.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理404异常
     *
     * @param e      404异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("资源不存在 - 请求路径: {}", request.getRequestURI());
        return ApiResponse.fail(ResultCode.NOT_FOUND);
    }

    /**
     * 处理未知异常
     *
     * @param e       异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 - 请求路径: {}, 异常信息: ", request.getRequestURI(), e);
        return ApiResponse.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}