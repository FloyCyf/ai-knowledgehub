package com.ai.knowledgehub.ai.service;

import com.ai.knowledgehub.ai.entity.ArticleAiTag;
import com.ai.knowledgehub.ai.entity.ArticleAuditResult;
import com.ai.knowledgehub.ai.mapper.ArticleAiTagMapper;
import com.ai.knowledgehub.ai.mapper.ArticleAuditResultMapper;
import com.ai.knowledgehub.ai.vo.AiTagVO;
import com.ai.knowledgehub.ai.vo.ArticleAnalysisVO;
import com.ai.knowledgehub.ai.vo.AuditResultVO;
import com.ai.knowledgehub.ai.vo.ContinueWritingVO;
import com.ai.knowledgehub.common.exception.BusinessException;
import com.ai.knowledgehub.common.result.ResultCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 服务
 * 使用 Mock LLM 实现 AI 续写、标签提取、合规检测功能
 * 生产环境可替换为真实 LLM API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ArticleAiTagMapper articleAiTagMapper;
    private final ArticleAuditResultMapper articleAuditResultMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.mock.model-name:MockLLM-v1}")
    private String modelName;

    // ==================== 续写相关 ====================

    /**
     * 同步续写
     *
     * @param prompt 续写提示
     * @return 续写结果
     */
    public ContinueWritingVO continueWriting(String prompt) {
        log.info("AI 续写请求, prompt: {}", prompt);

        String content = generateContent(prompt);

        ContinueWritingVO vo = new ContinueWritingVO();
        vo.setContent(content);
        vo.setModelName(modelName);
        return vo;
    }

    /**
     * 生成流式续写的 chunk 列表
     *
     * @param prompt 续写提示
     * @return 按 SSE 帧切分的文本块列表
     */
    public List<String> generateStreamingChunks(String prompt) {
        String fullContent = generateContent(prompt);
        return splitIntoChunks(fullContent);
    }

    /**
     * Mock LLM 生成续写内容
     * 根据关键词匹配返回预设模板文本
     */
    private String generateContent(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_VALID_ERROR, "续写提示不能为空");
        }

        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("redis")) {
            return "Redis ZSET 是一个有序集合，非常适合实现排行榜功能。"
                    + "通过 ZINCRBY 命令可以原子性地增加成员分数，"
                    + "通过 ZREVRANGE 命令可以按分数倒序获取排名。"
                    + "在实际项目中，我们可以将文章ID作为成员，热度分数作为score，"
                    + "这样就能高效地实现实时热榜功能。"
                    + "同时，Redis 的内存存储特性保证了极高的读写性能，"
                    + "非常适合高并发场景下的排行榜需求。";
        } else if (lowerPrompt.contains("spring")) {
            return "Spring Boot 通过自动配置极大地简化了企业级应用的开发。"
                    + "它采用约定大于配置的理念，开发者只需引入相应的 Starter 依赖，"
                    + "框架就会自动完成 Bean 的注册和配置。"
                    + "在微服务架构中，Spring Cloud 提供了服务发现、配置中心、"
                    + "网关、负载均衡等一站式解决方案，让微服务的开发和运维更加便捷。";
        } else if (lowerPrompt.contains("ai") || lowerPrompt.contains("人工智能") || lowerPrompt.contains("大模型")) {
            return "人工智能正在深刻改变软件开发的方式。"
                    + "大语言模型（LLM）如 GPT 系列展现了强大的文本理解和生成能力，"
                    + "可以辅助代码编写、文档生成、测试用例设计等工作。"
                    + "在实际应用中，通过 SSE 流式输出可以提供更好的用户体验，"
                    + "让用户实时看到 AI 生成的内容，而不是等待完整结果。"
                    + "未来，AI Agent 将能够自主完成更复杂的开发任务。";
        } else if (lowerPrompt.contains("mq") || lowerPrompt.contains("消息队列") || lowerPrompt.contains("rabbitmq")) {
            return "消息队列是分布式系统中实现解耦和异步处理的核心中间件。"
                    + "RabbitMQ 支持 Fanout Exchange 广播模式，"
                    + "可以将一条消息分发给多个消费者独立处理，互不影响。"
                    + "在本项目中，文章发布事件通过 Fanout Exchange 广播，"
                    + "标签提取和合规检测两个消费者各自独立消费，"
                    + "即使某个消费者处理失败也不会影响另一个。";
        } else {
            return "在现代后端开发中，微服务架构已经成为主流选择。"
                    + "通过将单体应用拆分为多个独立服务，"
                    + "每个服务可以独立开发、部署和扩展。"
                    + "Spring Cloud 提供了完善的微服务治理能力，"
                    + "包括服务发现、配置管理、网关路由和链路追踪等。"
                    + "同时，Docker 容器化技术让服务的部署和运维更加高效便捷。";
        }
    }

    /**
     * 将完整文本切分为 SSE 帧大小的 chunk
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int end = Math.min(pos + 6, text.length());
            // 优先在标点处切分
            int punctPos = findPunctuation(text, pos + 3, end);
            if (punctPos > pos) {
                end = punctPos + 1;
            }
            chunks.add(text.substring(pos, end));
            pos = end;
        }
        return chunks;
    }

    private int findPunctuation(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '，' || c == '。' || c == '、' || c == '；' || c == '！' || c == '？'
                    || c == ',' || c == '.' || c == ';' || c == '!' || c == '?') {
                return i;
            }
        }
        return -1;
    }

    // ==================== 标签提取 ====================

    /**
     * AI 标签提取（MQ 消费者调用）
     *
     * @param articleId 文章ID
     * @param title     文章标题
     * @param content   文章内容
     * @return 保存的标签实体
     */
    public ArticleAiTag extractTags(Long articleId, String title, String content) {
        log.info("开始提取文章标签, articleId: {}", articleId);

        // 幂等：已存在则跳过
        LambdaQueryWrapper<ArticleAiTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleAiTag::getArticleId, articleId)
                .orderByDesc(ArticleAiTag::getCreatedAt)
                .last("LIMIT 1");
        ArticleAiTag existing = articleAiTagMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("文章标签已存在，跳过提取, articleId: {}", articleId);
            return existing;
        }

        // Mock LLM 标签提取
        List<String> tags = mockExtractTags(title, content);

        // 序列化为 JSON 字符串
        String tagsJson;
        try {
            tagsJson = objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.error("标签序列化失败", e);
            tagsJson = "[]";
        }

        ArticleAiTag entity = new ArticleAiTag();
        entity.setArticleId(articleId);
        entity.setTags(tagsJson);
        entity.setModelName(modelName);
        entity.setCreatedAt(LocalDateTime.now());

        articleAiTagMapper.insert(entity);
        log.info("文章标签提取完成, articleId: {}, tags: {}", articleId, tags);
        return entity;
    }

    /**
     * Mock 标签提取逻辑
     * 根据标题和内容中的关键词匹配标签
     */
    private List<String> mockExtractTags(String title, String content) {
        String text = (title + " " + content).toLowerCase();

        // 关键词 -> 标签映射
        Map<String, List<String>> keywordTagMap = new LinkedHashMap<>();
        keywordTagMap.put("redis", List.of("Redis", "缓存", "NoSQL"));
        keywordTagMap.put("zset", List.of("ZSET", "有序集合", "排行榜"));
        keywordTagMap.put("spring", List.of("Spring Boot", "Java", "微服务"));
        keywordTagMap.put("mybatis", List.of("MyBatis", "ORM", "数据库"));
        keywordTagMap.put("ai", List.of("人工智能", "大模型", "深度学习"));
        keywordTagMap.put("rabbitmq", List.of("RabbitMQ", "消息队列", "异步"));
        keywordTagMap.put("docker", List.of("Docker", "容器化", "DevOps"));
        keywordTagMap.put("mysql", List.of("MySQL", "关系型数据库", "SQL"));
        keywordTagMap.put("jwt", List.of("JWT", "认证", "安全"));
        keywordTagMap.put("热榜", List.of("热榜", "排行榜", "实时"));

        List<String> matchedTags = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : keywordTagMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                matchedTags.addAll(entry.getValue());
            }
        }

        // 默认标签
        if (matchedTags.isEmpty()) {
            matchedTags.addAll(List.of("技术", "教程", "编程"));
        }

        // 去重并取前5个
        return matchedTags.stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    // ==================== 合规检测 ====================

    /**
     * AI 合规检测（MQ 消费者调用）
     *
     * @param articleId 文章ID
     * @param title     文章标题
     * @param content   文章内容
     * @return 保存的检测结果实体
     */
    public ArticleAuditResult auditArticle(Long articleId, String title, String content) {
        log.info("开始合规检测, articleId: {}", articleId);

        // 幂等：已存在则跳过
        LambdaQueryWrapper<ArticleAuditResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleAuditResult::getArticleId, articleId)
                .orderByDesc(ArticleAuditResult::getCreatedAt)
                .last("LIMIT 1");
        ArticleAuditResult existing = articleAuditResultMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("合规检测结果已存在，跳过检测, articleId: {}", articleId);
            return existing;
        }

        // Mock LLM 合规检测
        String result;
        String reason;

        String text = (title != null ? title : "") + " " + (content != null ? content : "");
        text = text.trim();

        if (text.length() < 10) {
            result = "REVIEW";
            reason = "文章内容过短，需人工审核";
        } else if (containsSensitiveWords(text)) {
            result = "REJECT";
            reason = "内容包含敏感词汇";
        } else {
            result = "PASS";
            reason = "内容合规";
        }

        ArticleAuditResult entity = new ArticleAuditResult();
        entity.setArticleId(articleId);
        entity.setResult(result);
        entity.setReason(reason);
        entity.setModelName(modelName);
        entity.setCreatedAt(LocalDateTime.now());

        articleAuditResultMapper.insert(entity);
        log.info("合规检测完成, articleId: {}, result: {}", articleId, result);
        return entity;
    }

    /**
     * Mock 敏感词检测
     */
    private boolean containsSensitiveWords(String text) {
        List<String> sensitiveWords = List.of("违规", "色情", "赌博", "诈骗", "毒品");
        String lowerText = text.toLowerCase();
        return sensitiveWords.stream().anyMatch(lowerText::contains);
    }

    // ==================== 分析结果查询 ====================

    /**
     * 获取文章 AI 分析结果
     *
     * @param articleId 文章ID
     * @return 包含标签和合规检测的分析结果
     */
    public ArticleAnalysisVO getArticleAnalysis(Long articleId) {
        log.info("查询文章AI分析结果, articleId: {}", articleId);

        // 查询最新标签
        LambdaQueryWrapper<ArticleAiTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(ArticleAiTag::getArticleId, articleId)
                .orderByDesc(ArticleAiTag::getCreatedAt)
                .last("LIMIT 1");
        ArticleAiTag tagEntity = articleAiTagMapper.selectOne(tagWrapper);

        // 查询最新合规检测结果
        LambdaQueryWrapper<ArticleAuditResult> auditWrapper = new LambdaQueryWrapper<>();
        auditWrapper.eq(ArticleAuditResult::getArticleId, articleId)
                .orderByDesc(ArticleAuditResult::getCreatedAt)
                .last("LIMIT 1");
        ArticleAuditResult auditEntity = articleAuditResultMapper.selectOne(auditWrapper);

        ArticleAnalysisVO vo = new ArticleAnalysisVO();
        vo.setTag(convertToTagVO(tagEntity));
        vo.setAudit(convertToAuditVO(auditEntity));
        return vo;
    }

    private AiTagVO convertToTagVO(ArticleAiTag entity) {
        if (entity == null) {
            return null;
        }
        AiTagVO vo = new AiTagVO();
        vo.setArticleId(entity.getArticleId());
        vo.setModelName(entity.getModelName());
        vo.setCreatedAt(entity.getCreatedAt());

        // 反序列化 JSON 字符串为标签列表
        try {
            List<String> tags = objectMapper.readValue(
                    entity.getTags(), new TypeReference<List<String>>() {});
            vo.setTags(tags);
        } catch (JsonProcessingException e) {
            log.warn("标签反序列化失败, tags: {}", entity.getTags(), e);
            vo.setTags(Collections.emptyList());
        }
        return vo;
    }

    private AuditResultVO convertToAuditVO(ArticleAuditResult entity) {
        if (entity == null) {
            return null;
        }
        AuditResultVO vo = new AuditResultVO();
        vo.setArticleId(entity.getArticleId());
        vo.setResult(entity.getResult());
        vo.setReason(entity.getReason());
        vo.setModelName(entity.getModelName());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
