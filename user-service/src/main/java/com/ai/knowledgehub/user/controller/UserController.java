package com.ai.knowledgehub.user.controller;

import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.result.ApiResponse;
import com.ai.knowledgehub.common.util.SecurityUtils;
import com.ai.knowledgehub.user.dto.LoginDTO;
import com.ai.knowledgehub.user.dto.RegisterDTO;
import com.ai.knowledgehub.user.service.UserService;
import com.ai.knowledgehub.user.vo.LoginVO;
import com.ai.knowledgehub.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户 Controller
 * <p>
 * 严格遵循 {@code docs/api-spec.md} 第 73~133 行的接口规范：
 * </p>
 * <ul>
 *     <li>所有方法返回 {@link ApiResponse}</li>
 *     <li>不写 try-catch，异常由 {@code GlobalExceptionHandler} 统一处理</li>
 *     <li>从 {@code SecurityUtils} 获取当前用户上下文（网关已写入 X-User-Id）</li>
 * </ul>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "注册、登录、注销、个人信息")
public class UserController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户名唯一，密码 BCrypt 加密入库")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto) {
        Long userId = userService.register(dto);
        log.info("用户注册成功: userId={}", userId);
        return ApiResponse.success(Map.of("userId", userId));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验密码后签发 JWT Token")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return ApiResponse.success("登录成功", userService.login(dto));
    }

    /**
     * 用户注销
     * <p>
     * 需要登录态（网关已校验 X-User-Id 存在）。
     * 从 Authorization 头拿 token，将其 jti 写入 Redis 黑名单。
     * </p>
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "将 token 加入 Redis 黑名单，使其立即失效")
    public ApiResponse<Void> logout(
            @Parameter(hidden = true)
            @RequestHeader(value = HeaderConstants.AUTHORIZATION, required = false) String authHeader) {
        String token = JwtUtil.extractToken(authHeader);
        userService.logout(token);
        return ApiResponse.success();
    }

    /**
     * 获取当前登录用户个人信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息", description = "需要登录态")
    public ApiResponse<UserVO> profile() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(userService.getProfile(userId));
    }
}
