package com.ai.knowledgehub.ai.controller;

import com.ai.knowledgehub.ai.dto.ContinueWritingDTO;
import com.ai.knowledgehub.ai.service.AiService;
import com.ai.knowledgehub.ai.vo.ArticleAnalysisVO;
import com.ai.knowledgehub.ai.vo.ContinueWritingVO;
import com.ai.knowledgehub.common.result.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI 控制器
 * 提供 AI 续写（同步/流式）和文章分析结果查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Value("${ai.mock.streaming-delay-ms:100}")
    private long streamingDelayMs;

    @Value("${ai.mock.timeout-ms:30000}")
    private long timeoutMs;

    /**
     * AI 续写（同步）
     *
     * @param dto 续写请求
     * @return 续写结果
     */
    @PostMapping("/continue-writing")
    public ApiResponse<ContinueWritingVO> continueWriting(@Valid @RequestBody ContinueWritingDTO dto) {
        ContinueWritingVO vo = aiService.continueWriting(dto.getPrompt());
        return ApiResponse.success(vo);
    }

    /**
     * AI 续写（SSE 流式）
     * 返回 text/event-stream，逐字/逐句输出续写内容
     *
     * @param prompt 续写提示
     * @return SseEmitter
     */
    @GetMapping(value = "/continue-writing/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueWritingStream(@RequestParam String prompt) {
        log.info("SSE 流式续写请求, prompt: {}", prompt);

        SseEmitter emitter = new SseEmitter(timeoutMs);

        // 注册生命周期回调
        emitter.onCompletion(() -> log.info("SSE 连接完成, prompt: {}", prompt));
        emitter.onTimeout(() -> log.warn("SSE 连接超时, prompt: {}", prompt));
        emitter.onError(e -> log.error("SSE 连接异常, prompt: {}", prompt, e));

        // 异步执行流式输出
        CompletableFuture.runAsync(() -> {
            try {
                List<String> chunks = aiService.generateStreamingChunks(prompt);
                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event().data(chunk));
                    Thread.sleep(streamingDelayMs);
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流式输出异常", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 获取文章 AI 分析结果（标签 + 合规检测）
     *
     * @param id 文章ID
     * @return 分析结果
     */
    @GetMapping("/articles/{id}/analysis")
    public ApiResponse<ArticleAnalysisVO> getArticleAnalysis(@PathVariable Long id) {
        ArticleAnalysisVO vo = aiService.getArticleAnalysis(id);
        return ApiResponse.success(vo);
    }
}
