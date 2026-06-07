package com.ai.knowledgehub.article.mapper;

import com.ai.knowledgehub.article.entity.ArticleLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞 Mapper 接口
 */
@Mapper
public interface LikeMapper extends BaseMapper<ArticleLike> {
}