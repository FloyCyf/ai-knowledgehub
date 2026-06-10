package com.ai.knowledgehub.article.mapper;

import com.ai.knowledgehub.article.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 标签Mapper
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 根据名称查询标签
     */
    Tag selectByName(@Param("name") String name);

    /**
     * 增加使用次数
     */
    @Update("UPDATE tag SET usage_count = usage_count + 1 WHERE id = #{id}")
    void incrementUsageCount(@Param("id") Long id);

    /**
     * 减少使用次数
     */
    @Update("UPDATE tag SET usage_count = usage_count - 1 WHERE id = #{id} AND usage_count > 0")
    void decrementUsageCount(@Param("id") Long id);
}