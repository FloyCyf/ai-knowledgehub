package com.ai.knowledgehub.article.service;

import com.ai.knowledgehub.article.dto.ArticleDTO;
import com.ai.knowledgehub.article.entity.Article;
import com.ai.knowledgehub.article.mapper.ArticleMapper;
import com.ai.knowledgehub.article.vo.ArticleVO;
import com.ai.knowledgehub.article.config.MqConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文章服务
 * 处理文章的创建、修改、发布、删除、查询等业务逻辑
 */
@Slf4j
@Service
public class ArticleService {

    private final ArticleMapper articleMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    private final RankingService rankingService;

    public ArticleService(ArticleMapper articleMapper, RankingService rankingService) {
        this.articleMapper = articleMapper;
        this.rankingService = rankingService;
    }

    // Redis 热榜 Key
    private static final String HOT_RANKING_KEY = "article:hot:ranking";

    /**
     * 创建文章草稿
     *
     * @param articleDTO 文章数据
     * @param authorId    作者 ID
     * @return 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(ArticleDTO articleDTO, Long authorId) {
        Article article = new Article();
        article.setAuthorId(authorId);
        article.setTitle(articleDTO.getTitle());
        article.setContent(articleDTO.getContent());
        article.setSummary(articleDTO.getSummary());
        article.setStatus("DRAFT");
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCommentCount(0L);
        article.setDeleted(0);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        articleMapper.insert(article);
        log.info("创建文章草稿成功，文章ID: {}, 作者ID: {}", article.getId(), authorId);
        return article.getId();
    }

    /**
     * 修改文章
     *
     * @param articleId  文章 ID
     * @param articleDTO 文章数据
     * @param userId     当前用户 ID
     * @param userRole   当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long articleId, ArticleDTO articleDTO, Long userId, String userRole) {
        Article article = getArticleById(articleId);

        // 权限校验：只有作者和管理员可以修改
        if (!article.getAuthorId().equals(userId) && !"ADMIN".equals(userRole)) {
            throw new RuntimeException("无权限修改该文章");
        }

        // 已删除的文章不能修改
        if (article.getDeleted() == 1) {
            throw new RuntimeException("文章已被删除，无法修改");
        }

        article.setTitle(articleDTO.getTitle());
        article.setContent(articleDTO.getContent());
        article.setSummary(articleDTO.getSummary());
        article.setUpdatedAt(LocalDateTime.now());

        articleMapper.updateById(article);
        log.info("修改文章成功，文章ID: {}", articleId);
    }

    /**
     * 发布文章
     *
     * @param articleId 文章 ID
     * @param userId     当前用户 ID
     * @param userRole   当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(Long articleId, Long userId, String userRole) {
        Article article = getArticleById(articleId);

        // 权限校验：只有作者和管理员可以发布
        if (!article.getAuthorId().equals(userId) && !"ADMIN".equals(userRole)) {
            throw new RuntimeException("无权限发布该文章");
        }

        // 已删除的文章不能发布
        if (article.getDeleted() == 1) {
            throw new RuntimeException("文章已被删除，无法发布");
        }

        // 更新文章状态
        article.setStatus("PUBLISHED");
        article.setPublishedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);

        // 发布热度 +2
        rankingService.increaseScore(articleId, 2);

        // 发送 MQ 消息
        sendArticlePublishedEvent(article);

        log.info("发布文章成功，文章ID: {}", articleId);
    }

    /**
     * 逻辑删除文章
     *
     * @param articleId 文章 ID
     * @param userId    当前用户 ID
     * @param userRole   当前用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long articleId, Long userId, String userRole) {
        Article article = getArticleById(articleId);

        // 权限校验：只有作者和管理员可以删除
        if (!article.getAuthorId().equals(userId) && !"ADMIN".equals(userRole)) {
            throw new RuntimeException("无权限删除该文章");
        }

        // 已删除的文章不能重复删除
        if (article.getDeleted() == 1) {
            throw new RuntimeException("文章已被删除");
        }

        article.setDeleted(1);
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);

        log.info("删除文章成功，文章ID: {}", articleId);
    }

    /**
     * 分页获取最新文章列表
     *
     * @param page 页码
     * @param size 每页数量
     * @return 文章列表
     */
    public IPage<ArticleVO> getLatestArticles(int page, int size) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getStatus, "PUBLISHED")
                .eq(Article::getDeleted, 0)
                .orderByDesc(Article::getPublishedAt);

        IPage<Article> articlePage = articleMapper.selectPage(pageParam, queryWrapper);

        // 转换为 VO
        IPage<ArticleVO> voPage = articlePage.convert(this::convertToVO);
        return voPage;
    }

    /**
     * 获取文章详情
     *
     * @param articleId 文章 ID
     * @return 文章详情
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO getArticleDetail(Long articleId) {
        Article article = getArticleById(articleId);

        // 已删除的文章不能查看
        if (article.getDeleted() == 1) {
            throw new RuntimeException("文章不存在");
        }

        // 阅读量 +1
        article.setViewCount(article.getViewCount() + 1);
        articleMapper.updateById(article);

        // 热度 +1
        rankingService.increaseScore(articleId, 1);

        return convertToVO(article);
    }

    /**
     * 根据 ID 获取文章
     *
     * @param articleId 文章 ID
     * @return 文章实体
     */
    public Article getArticleById(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        return article;
    }

    /**
     * 增加评论数
     *
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementCommentCount(Long articleId) {
        Article article = getArticleById(articleId);
        article.setCommentCount(article.getCommentCount() + 1);
        articleMapper.updateById(article);
    }

    /**
     * 增加点赞数
     *
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long articleId) {
        Article article = getArticleById(articleId);
        article.setLikeCount(article.getLikeCount() + 1);
        articleMapper.updateById(article);
    }

    /**
     * 发送文章发布事件到 MQ
     *
     * @param article 文章实体
     */
    private void sendArticlePublishedEvent(Article article) {
        if (rabbitTemplate != null) {
            Map<String, Object> event = new HashMap<>();
            event.put("articleId", article.getId());
            event.put("title", article.getTitle());
            event.put("authorId", article.getAuthorId());
            event.put("content", article.getContent());
            event.put("publishTime", article.getPublishedAt());

            rabbitTemplate.convertAndSend(MqConfig.ARTICLE_PUBLISH_EXCHANGE, "", event);
            log.info("发送文章发布事件，文章ID: {}", article.getId());
        } else {
            log.info("RabbitMQ 未配置，跳过发送文章发布事件，文章ID: {}", article.getId());
        }
    }

    /**
     * 转换为 VO
     *
     * @param article 文章实体
     * @return 文章 VO
     */
    private ArticleVO convertToVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);
        return vo;
    }
}