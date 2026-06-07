package com.ai.knowledgehub.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回 VO
 *
 * @author AI KnowledgeHub Team
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "登录成功响应")
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JWT Token
     */
    @Schema(description = "JWT Token")
    private String token;

    /**
     * 过期时间（毫秒时间戳）
     */
    @Schema(description = "Token 过期时间（毫秒）", example = "1718000000000")
    private Long expiration;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息")
    private UserVO user;
}
