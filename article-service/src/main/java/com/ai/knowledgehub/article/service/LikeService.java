package com.ai.knowledgehub.article.service;

import com.ai.knowledgehub.article.client.RankingClient;
import com.ai.knowledgehub.article.entity.ArticleLike;
import com.ai.knowledgehub.article.mapper.LikeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 点赞服务
 * 处理文章点赞功能，防止重复点赞
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;
    private final ArticleService articleService;
    private final RankingClient rankingClient;

    /**
     * 点赞文章
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否点赞成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean likeArticle(Long articleId, Long userId) {
        // 验证文章是否存在
        articleService.getArticleById(articleId);

        // 检查是否已点赞
        LambdaQueryWrapper<ArticleLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId);
        ArticleLike existingLike = likeMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            log.warn("用户已点赞过该文章，文章ID: {}, 用户ID: {}", articleId, userId);
            throw new RuntimeException("您已点赞过该文章");
        }

        // 创建点赞记录
        ArticleLike articleLike = new ArticleLike();
        articleLike.setArticleId(articleId);
        articleLike.setUserId(userId);
        articleLike.setCreatedAt(LocalDateTime.now());

        try {
            likeMapper.insert(articleLike);
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，说明已点赞
            log.warn("重复点赞，文章ID: {}, 用户ID: {}", articleId, userId);
            throw new RuntimeException("您已点赞过该文章");
        }

        // 增加文章点赞数
        articleService.incrementLikeCount(articleId);

        // 通知 ranking-service 增加点赞热度
        rankingClient.notifyLike(articleId);

        log.info("点赞成功，文章ID: {}, 用户ID: {}", articleId, userId);
        return true;
    }

    /**
     * 检查用户是否已点赞文章
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否已点赞
     */
    public boolean hasLiked(Long articleId, Long userId) {
        LambdaQueryWrapper<ArticleLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId);
        return likeMapper.selectCount(queryWrapper) > 0;
    }
}