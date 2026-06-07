package com.ai.knowledgehub.user.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户返回 VO（不含密码哈希）
 *
 * @author AI KnowledgeHub Team
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色：USER / ADMIN")
    private String role;

    @Schema(description = "状态：ENABLED / DISABLED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
