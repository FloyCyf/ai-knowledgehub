package com.ai.knowledgehub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求 DTO
 *
 * @author AI KnowledgeHub Team
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名（4~50 字符，仅允许字母/数字/下划线/中文）
     */
    @Schema(description = "用户名", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在 4~50 之间")
    @Pattern(regexp = "^[A-Za-z0-9_\\u4e00-\\u9fa5]+$",
            message = "用户名只能包含字母、数字、下划线、中文")
    private String username;

    /**
     * 密码（6~50 字符）
     */
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在 6~50 之间")
    private String password;
}
