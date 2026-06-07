package com.ai.knowledgehub.common.config;

import com.ai.knowledgehub.common.util.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 配合 {@code BaseEntity} 中的 {@code @TableField(fill = ...)} 注解使用：
 * </p>
 * <ul>
 *     <li>INSERT 时自动填充：createTime / updateTime / createBy / updateBy</li>
 *     <li>UPDATE 时自动填充：updateTime / updateBy</li>
 * </ul>
 * <p>
 * 字段不存在于实体或没有 fill 注解时由父类 {@link MetaObjectHandler#strictInsertFill}
 * 静默跳过，不会抛异常。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     *
     * @param metaObject MyBatis 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始 INSERT 自动填充");
        Long currentUserId = currentUserIdOrZero();
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject MyBatis 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始 UPDATE 自动填充");
        Long currentUserId = currentUserIdOrZero();
        LocalDateTime now = LocalDateTime.now();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 获取当前登录用户 ID，未登录填 0
     */
    private Long currentUserIdOrZero() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            return userId != null ? userId : 0L;
        } catch (Exception e) {
            // SecurityUtils 在非 Web 环境（如批处理、定时任务）会抛异常，吞掉即可
            return 0L;
        }
    }
}
