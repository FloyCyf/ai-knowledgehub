package com.ai.knowledgehub.article.mapper;

import com.ai.knowledgehub.article.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 Mapper 接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}