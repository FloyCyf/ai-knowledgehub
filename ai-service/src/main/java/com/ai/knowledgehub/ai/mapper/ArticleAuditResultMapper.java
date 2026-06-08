package com.ai.knowledgehub.ai.mapper;

import com.ai.knowledgehub.ai.entity.ArticleAuditResult;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章 AI 合规检测结果 Mapper
 */
@Mapper
public interface ArticleAuditResultMapper extends BaseMapper<ArticleAuditResult> {
}
