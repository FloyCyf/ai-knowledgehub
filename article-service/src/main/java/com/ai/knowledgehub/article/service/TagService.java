package com.ai.knowledgehub.article.service;

import com.ai.knowledgehub.article.dto.TagDTO;
import com.ai.knowledgehub.article.entity.ArticleTag;
import com.ai.knowledgehub.article.entity.Tag;
import com.ai.knowledgehub.article.mapper.ArticleTagMapper;
import com.ai.knowledgehub.article.mapper.TagMapper;
import com.ai.knowledgehub.article.vo.TagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(TagDTO tagDTO) {
        // 检查标签是否已存在
        Tag existingTag = tagMapper.selectByName(tagDTO.getName());
        if (existingTag != null) {
            throw new RuntimeException("标签已存在");
        }

        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor() != null ? tagDTO.getColor() : getRandomColor());
        tag.setUsageCount(0);

        tagMapper.insert(tag);
        log.info("创建标签成功，标签ID: {}, 名称: {}", tag.getId(), tag.getName());
        return tag.getId();
    }

    /**
     * 删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }

        // 删除文章标签关联
        articleTagMapper.deleteByTagId(tagId);
        // 删除标签
        tagMapper.deleteById(tagId);

        log.info("删除标签成功，标签ID: {}", tagId);
    }

    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(Long tagId, TagDTO tagDTO) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }

        tag.setName(tagDTO.getName());
        if (tagDTO.getColor() != null) {
            tag.setColor(tagDTO.getColor());
        }

        tagMapper.updateById(tag);
        log.info("更新标签成功，标签ID: {}", tagId);
    }

    /**
     * 分页获取标签列表
     */
    public IPage<TagVO> getTagList(int page, int size) {
        Page<Tag> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Tag> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Tag::getCreateTime);

        IPage<Tag> tagPage = tagMapper.selectPage(pageParam, queryWrapper);
        return tagPage.convert(this::convertToVO);
    }

    /**
     * 根据ID获取标签
     */
    public TagVO getTagById(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        return convertToVO(tag);
    }

    /**
     * 为文章添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTagsToArticle(Long articleId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        // 删除文章原有标签
        articleTagMapper.deleteByArticleId(articleId);

        for (String tagName : tagNames) {
            // 查找或创建标签
            Tag tag = tagMapper.selectByName(tagName);
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tag.setColor(getRandomColor());
                tag.setUsageCount(0);
                tagMapper.insert(tag);
            }

            // 创建关联
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(articleId);
            articleTag.setTagId(tag.getId());
            articleTag.setCreatedAt(LocalDateTime.now());
            articleTagMapper.insert(articleTag);

            // 增加标签使用次数
            tagMapper.incrementUsageCount(tag.getId());
        }

        log.info("为文章添加标签成功，文章ID: {}, 标签数量: {}", articleId, tagNames.size());
    }

    /**
     * 获取文章的标签列表
     */
    public List<TagVO> getArticleTags(Long articleId) {
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(articleId);
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }

        return tagIds.stream()
                .map(tagId -> {
                    Tag tag = tagMapper.selectById(tagId);
                    return tag != null ? convertToVO(tag) : null;
                })
                .filter(tagVO -> tagVO != null)
                .collect(Collectors.toList());
    }

    /**
     * 移除文章的标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeTagsFromArticle(Long articleId) {
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(articleId);
        
        // 减少标签使用次数
        tagIds.forEach(tagMapper::decrementUsageCount);
        
        // 删除关联
        articleTagMapper.deleteByArticleId(articleId);

        log.info("移除文章标签成功，文章ID: {}", articleId);
    }

    /**
     * 获取随机颜色
     */
    private String getRandomColor() {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", 
                          "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"};
        return colors[(int) (Math.random() * colors.length)];
    }

    /**
     * 转换为VO
     */
    private TagVO convertToVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }
}