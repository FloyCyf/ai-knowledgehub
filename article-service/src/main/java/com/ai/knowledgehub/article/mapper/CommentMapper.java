package com.ai.knowledgehub.article.mapper;

import com.ai.knowledgehub.article.entity.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}