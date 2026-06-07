package com.ai.knowledgehub.user.entity;

import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.ai.knowledgehub.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户实体
 * <p>
 * 严格遵循 {@code docs/database-design.md} 第 7~16 行定义。
 * 继承 {@link BaseEntity} 以获得自动填充（createTime / updateTime / createBy / updateBy）。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名（唯一，长度 4~50）
     */
    private String username;

    /**
     * BCrypt 加密后的密码哈希（长度 60+，{@code $2a$10$...} 开头）
     * <p>标记 {@link JsonIgnore} 防止意外序列化泄露</p>
     */
    @JsonIgnore
    private String passwordHash;

    /**
     * 角色：USER / ADMIN
     */
    private String role;

    /**
     * 状态：ENABLED / DISABLED
     */
    private String status;

    // ============== 静态工厂 ==============

    /**
     * 校验角色是否为管理员
     */
    public boolean isAdmin() {
        return HeaderConstants.ROLE_ADMIN.equals(role);
    }

    /**
     * 校验账号是否启用
     */
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }

    /**
     * 用户角色枚举（保留字段，便于未来重构）
     */
    public enum Role {
        USER, ADMIN;

        @EnumValue
        public String getCode() {
            return name();
        }
    }

    /**
     * 用户状态枚举
     */
    public enum Status {
        ENABLED, DISABLED;

        @EnumValue
        public String getCode() {
            return name();
        }
    }
}
