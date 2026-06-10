package com.ai.knowledgehub.article.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 敏感词过滤服务
 */
@Slf4j
@Service
public class SensitiveWordService {

    /**
     * 敏感词字典树
     */
    private final Map<Character, Map> sensitiveTree = new HashMap<>();

    /**
     * 敏感词列表
     */
    private final Set<String> sensitiveWords = new HashSet<>();

    /**
     * 初始化敏感词库
     */
    @PostConstruct
    public void init() {
        // 加载默认敏感词
        loadDefaultSensitiveWords();
        buildSensitiveTree();
        log.info("敏感词过滤服务初始化完成，敏感词数量: {}", sensitiveWords.size());
    }

    /**
     * 加载默认敏感词
     */
    private void loadDefaultSensitiveWords() {
        // 示例敏感词（实际项目中应从配置文件或数据库加载）
        String[] words = {
            "敏感词1", "敏感词2", "敏感词3", "非法", "暴力", "色情", 
            "赌博", "毒品", "反动", "恐怖", "邪教", "辱骂", "恶意"
        };
        sensitiveWords.addAll(Arrays.asList(words));
    }

    /**
     * 构建敏感词字典树
     */
    private void buildSensitiveTree() {
        for (String word : sensitiveWords) {
            Map<Character, Map> current = sensitiveTree;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (!current.containsKey(c)) {
                    current.put(c, new HashMap<>());
                }
                current = current.get(c);
                // 标记是否为词尾
                if (i == word.length() - 1) {
                    current.put('*', null);
                }
            }
        }
    }

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (int i = 0; i < text.length(); i++) {
            if (checkSensitiveWord(text, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从指定位置开始检测敏感词
     */
    private boolean checkSensitiveWord(String text, int startIndex) {
        Map<Character, Map> current = sensitiveTree;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!current.containsKey(c)) {
                break;
            }
            current = current.get(c);
            if (current.containsKey('*')) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取文本中包含的所有敏感词
     *
     * @param text 待检测文本
     * @return 敏感词列表
     */
    public List<String> findSensitiveWords(String text) {
        List<String> found = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return found;
        }

        for (int i = 0; i < text.length(); i++) {
            String word = findSensitiveWord(text, i);
            if (word != null && !found.contains(word)) {
                found.add(word);
            }
        }
        return found;
    }

    /**
     * 从指定位置查找敏感词
     */
    private String findSensitiveWord(String text, int startIndex) {
        Map<Character, Map> current = sensitiveTree;
        StringBuilder word = new StringBuilder();

        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!current.containsKey(c)) {
                break;
            }
            word.append(c);
            current = current.get(c);
            if (current.containsKey('*')) {
                return word.toString();
            }
        }
        return null;
    }

    /**
     * 替换敏感词
     *
     * @param text      待处理文本
     * @param replaceChar 替换字符
     * @return 处理后的文本
     */
    public String replaceSensitiveWords(String text, char replaceChar) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder(text);
        List<int[]> positions = new ArrayList<>();

        // 查找所有敏感词位置
        for (int i = 0; i < text.length(); i++) {
            String word = findSensitiveWord(text, i);
            if (word != null) {
                positions.add(new int[]{i, i + word.length()});
                i += word.length() - 1;
            }
        }

        // 替换敏感词
        for (int[] pos : positions) {
            for (int i = pos[0]; i < pos[1]; i++) {
                result.setCharAt(i, replaceChar);
            }
        }

        return result.toString();
    }

    /**
     * 添加敏感词
     */
    public void addSensitiveWord(String word) {
        if (word != null && !word.isEmpty()) {
            sensitiveWords.add(word);
            buildSensitiveTree();
            log.info("添加敏感词: {}", word);
        }
    }

    /**
     * 删除敏感词
     */
    public void removeSensitiveWord(String word) {
        if (word != null && sensitiveWords.remove(word)) {
            buildSensitiveTree();
            log.info("删除敏感词: {}", word);
        }
    }

    /**
     * 获取所有敏感词
     */
    public Set<String> getAllSensitiveWords() {
        return new HashSet<>(sensitiveWords);
    }
}