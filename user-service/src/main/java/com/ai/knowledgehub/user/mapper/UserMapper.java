package com.ai.knowledgehub.user.mapper;

import com.ai.knowledgehub.user.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * <p>
 * 继承 MyBatis-Plus {@link BaseMapper}，自带 CRUD 能力。
 * 自定义复杂查询使用 XML。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按用户名查询（大小写不敏感）
     *
     * @param username 用户名
     * @return 用户实体
     */
    default User selectByUsername(String username) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }
}
