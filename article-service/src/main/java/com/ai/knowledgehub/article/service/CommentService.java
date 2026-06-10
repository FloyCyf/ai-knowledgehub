package com.ai.knowledgehub.article.service;

import com.ai.knowledgehub.article.client.RankingClient;
import com.ai.knowledgehub.article.dto.CommentDTO;
import com.ai.knowledgehub.article.entity.Comment;
import com.ai.knowledgehub.article.mapper.CommentMapper;
import com.ai.knowledgehub.article.vo.CommentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论服务
 * 处理评论的创建和查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleService articleService;
    private final RankingClient rankingClient;
    private final SensitiveWordService sensitiveWordService;

    /**
     * 创建评论
     *
     * @param articleId  文章 ID
     * @param commentDTO 评论数据
     * @param userId     用户 ID
     * @return 评论 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(Long articleId, CommentDTO commentDTO, Long userId) {
        // 验证文章是否存在
        articleService.getArticleById(articleId);

        // 敏感词过滤
        String content = commentDTO.getContent();
        List<String> sensitiveWords = sensitiveWordService.findSensitiveWords(content);
        
        if (!sensitiveWords.isEmpty()) {
            log.warn("评论包含敏感词，文章ID: {}, 用户ID: {}, 敏感词: {}", articleId, userId, sensitiveWords);
            throw new RuntimeException("评论内容包含敏感词");
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setDeleted(0);
        comment.setCreatedAt(LocalDateTime.now());

        commentMapper.insert(comment);

        // 增加文章评论数
        articleService.incrementCommentCount(articleId);

        // 通知 ranking-service 增加评论热度
        rankingClient.notifyComment(articleId);

        log.info("创建评论成功，评论ID: {}, 文章ID: {}, 用户ID: {}", comment.getId(), articleId, userId);
        return comment.getId();
    }

    /**
     * 分页获取文章评论列表
     *
     * @param articleId 文章 ID
     * @param page      页码
     * @param size      每页数量
     * @return 评论列表
     */
    public IPage<CommentVO> getCommentList(Long articleId, int page, int size) {
        Page<Comment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId)
                .eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreatedAt);

        IPage<Comment> commentPage = commentMapper.selectPage(pageParam, queryWrapper);

        // 转换为 VO
        IPage<CommentVO> voPage = commentPage.convert(this::convertToVO);
        return voPage;
    }

    /**
     * 转换为 VO
     *
     * @param comment 评论实体
     * @return 评论 VO
     */
    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        return vo;
    }
}