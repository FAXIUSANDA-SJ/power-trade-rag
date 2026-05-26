package com.powertrade.api.controller;

import com.powertrade.core.model.ChatRequest;
import com.powertrade.core.model.ChatResponse;
import com.powertrade.core.service.rag.RagCoreService;
import com.powertrade.core.service.rag.SimilaritySearchEngine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 智能问答控制器
 * 基于完整的 RAG 流程实现
 */
@RestController
@RequestMapping("/api/chat")
@Api(tags = "智能问答接口")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private RagCoreService ragCoreService;

    @PostMapping("/ask")
    @ApiOperation("智能问答")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        log.info("收到问答请求，问题：{}", request.getQuery());
        
        ChatResponse response = ragCoreService.chat(request);
        
        log.info("问答响应完成，状态码：{}", response.getCode());
        
        return response;
    }

    @PostMapping("/search")
    @ApiOperation("测试检索（不生成答案）")
    public SimilaritySearchEngine.SearchResult testSearch(
            @RequestParam String query,
            @RequestParam(required = false) String kbId) {
        
        log.info("收到测试检索请求，查询：{}", query);
        
        return ragCoreService.testSearch(query, kbId);
    }

    @DeleteMapping("/session/{sessionId}")
    @ApiOperation("清空指定会话的记忆")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        ragCoreService.clearSession(sessionId);
        return Collections.<String, Object>singletonMap("message", "会话记忆已清空");
    }
}
