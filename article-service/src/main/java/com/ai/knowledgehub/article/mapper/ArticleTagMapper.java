package com.ai.knowledgehub.article.mapper;

import com.ai.knowledgehub.article.entity.ArticleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章标签关联Mapper
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    /**
     * 根据文章ID查询标签ID列表
     */
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据文章ID删除所有关联
     */
    void deleteByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据标签ID删除所有关联
     */
    void deleteByTagId(@Param("tagId") Long tagId);

    /**
     * 查询使用该标签的文章数量
     */
    int countByTagId(@Param("tagId") Long tagId);
}