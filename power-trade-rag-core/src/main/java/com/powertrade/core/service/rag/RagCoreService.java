package com.powertrade.core.service.rag;

import com.powertrade.core.model.ChatRequest;
import com.powertrade.core.model.ChatResponse;
import com.powertrade.core.model.KnowledgeBaseStats;
import com.powertrade.core.model.PromptConfig;
import com.powertrade.core.model.RetrievalConfig;
import com.powertrade.core.service.config.RetrievalConfigService;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG 核心服务
 * 整合文档预处理、向量存储、相似度检索和答案生成的完整流程
 * 基于 OpenAI text-embedding-ada-002 和 ChromaDB/Milvus
 */
@Service
public class RagCoreService {

    private static final Logger log = LoggerFactory.getLogger(RagCoreService.class);

    @Autowired
    private DocumentPreprocessingService preprocessingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private SimilaritySearchEngine searchEngine;

    @Autowired
    private AnswerGenerationService answerGenerationService;

    @Autowired
    private ConversationMemoryService conversationMemoryService;

    @Autowired
    private PromptConfigService promptConfigService;

    @Autowired
    private RetrievalConfigService retrievalConfigService;

    /**
     * 完整的 RAG 问答流程
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        log.info("=================================");
        log.info("开始 RAG 问答流程");
        log.info("使用向量数据库：ChromaDB/Milvus");
        log.info("嵌入模型：text-embedding-ada-002");
        log.info("用户问题：{}", request.getQuery());
        log.info("知识库 ID: {}", request.getKbId());
        log.info("=================================");

        ChatResponse response = new ChatResponse();

        try {
            // 1. 生成或复用会话 ID
            String sessionId = conversationMemoryService.getOrCreateSessionId(request.getSessionId());
            response.setSessionId(sessionId);

            String query = request.getQuery();
            String kbId = request.getKbId();
            PromptConfig promptConfig = promptConfigService.getConfig();
            RetrievalConfig retrievalConfig = retrievalConfigService.getActiveConfig();

            // 2. 相似度检索（使用 OpenAI 嵌入模型）
            log.info("步骤 1: 执行相似度检索");
            SimilaritySearchEngine.SearchRequest searchRequest = searchEngine.builder()
                    .query(query)
                    .maxResults(retrievalConfig.getTopK())
                    .kbId(kbId)
                    .minScore(retrievalConfig.getMinScore());

            SimilaritySearchEngine.SearchResult searchResult = searchEngine.search(searchRequest);
            log.info("检索到 {} 个相关文档", searchResult.getTotalMatches());

            // 3. 检查是否有匹配结果
            if (searchResult.getDocuments().isEmpty()) {
                log.warn("未找到相关文档，使用通用回答");
                String fallbackReply = promptConfig.getFallbackReply();
                response.setAnswer(fallbackReply);
                response.setReferences(new ArrayList<>());
                response.setCode(200);
                response.setMessage("未找到相关文档");
                conversationMemoryService.appendConversation(sessionId, query, fallbackReply);
                return response;
            }

            // 4. 构建参考文档列表
            List<String> references = searchResult.getDocuments().stream()
                    .map(SimilaritySearchEngine.MatchedDocument::getDocId)
                    .collect(Collectors.toList());

            // 5. 生成答案（使用检索到的上下文）
            log.info("步骤 2: 生成答案");
            List<ConversationMemoryService.ConversationTurn> conversationHistory =
                    conversationMemoryService.getRecentHistory(sessionId);
            String answer = answerGenerationService.generateAnswer(query, searchResult.getDocuments(), conversationHistory);

            // 6. 设置响应
            response.setAnswer(answer);
            response.setReferences(references);
            response.setCode(200);
            response.setMessage("success");
            conversationMemoryService.appendConversation(sessionId, query, answer);

            log.info("RAG 问答流程完成");
            log.info("=================================");

            return response;

        } catch (Exception e) {
            log.error("RAG 问答流程失败：{}", e.getMessage(), e);
            response.setAnswer("抱歉，系统出现错误，请稍后再试。");
            response.setReferences(new ArrayList<>());
            response.setCode(500);
            response.setMessage("系统错误：" + e.getMessage());
            return response;
        }
    }

    public void clearSession(String sessionId) {
        conversationMemoryService.clearSession(sessionId);
    }

    public SimilaritySearchEngine.SearchResult testSearch(String query, String kbId) {
        RetrievalConfig retrievalConfig = retrievalConfigService.getActiveConfig();
        SimilaritySearchEngine.SearchRequest searchRequest = searchEngine.builder()
                .query(query)
                .maxResults(retrievalConfig.getTopK())
                .kbId(kbId)
                .minScore(retrievalConfig.getMinScore());
        return searchEngine.search(searchRequest);
    }

    /**
     * 处理并存储文档
     * @param file 上传的文件
     * @param kbId 知识库 ID
     * @param docId 文档 ID
     * @return 文本片段数量
     */
    public int processAndStoreDocument(MultipartFile file, String kbId, String docId) {
        log.info("=================================");
        log.info("开始处理文档");
        log.info("文档 ID: {}", docId);
        log.info("知识库 ID: {}", kbId);
        log.info("文件名：{}", file.getOriginalFilename());
        log.info("=================================");

        try {
            byte[] fileBytes = file.getBytes();
            String content = new String(fileBytes, "UTF-8");
            return processAndStoreDocumentContent(content, kbId, docId, file.getOriginalFilename());

        } catch (IOException e) {
            log.error("文档处理失败：{}", e.getMessage(), e);
            throw new RuntimeException("文档处理失败：" + e.getMessage(), e);
        }
    }

    public int processAndStoreDocumentContent(String content, String kbId, String docId, String fileName) {
        log.info("步骤 1: 文档预处理和分块");
        List<TextSegment> segments = preprocessingService.preprocessText(content, docId, kbId);
        log.info("文档被分为 {} 个片段", segments.size());

        preprocessingService.addMetadataToSegments(segments, docId, kbId, fileName);

        if (segments.isEmpty()) {
            throw new RuntimeException("文档分块失败，未生成任何片段");
        }

        log.info("步骤 2: 向量化并存储到向量数据库");
        List<String> vectorIds = vectorStoreService.addTextSegments(segments, kbId);
        log.info("生成 {} 个向量，存储成功", vectorIds.size());
        log.info("文档处理完成");
        log.info("=================================");
        return segments.size();
    }

    /**
     * 处理并存储纯文本
     * @param text 文本内容
     * @param kbId 知识库 ID
     * @param title 文本标题
     * @return 文本片段数量
     */
    public int processAndStoreText(String text, String kbId, String title) {
        log.info("=================================");
        log.info("开始处理文本");
        log.info("知识库 ID: {}", kbId);
        log.info("标题：{}", title);
        log.info("=================================");

        try {
            // 1. 生成文档 ID
            String docId = "TEXT_" + UUID.randomUUID().toString();

            // 2. 文档预处理（分块）
            log.info("步骤 1: 文本预处理和分块");
            List<TextSegment> segments = preprocessingService.preprocessText(text, docId, kbId);
            log.info("文本被分为 {} 个片段", segments.size());

            preprocessingService.addMetadataToSegments(segments, docId, kbId, title);

            if (segments.isEmpty()) {
                throw new RuntimeException("文本分块失败，未生成任何片段");
            }

            // 3. 向量化并存储（使用 OpenAI text-embedding-ada-002）
            log.info("步骤 2: 向量化并存储到向量数据库");
            List<String> vectorIds = vectorStoreService.addTextSegments(segments, kbId);
            log.info("生成 {} 个向量，存储成功", vectorIds.size());

            log.info("文本处理完成");
            log.info("=================================");

            return segments.size();

        } catch (Exception e) {
            log.error("文本处理失败：{}", e.getMessage(), e);
            throw new RuntimeException("文本处理失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除文档及其向量
     * @param docId 文档 ID
     */
    public void deleteDocument(String docId) {
        deleteDocument(docId, null);
    }

    /**
     * 删除文档及其向量
     * @param docId 文档 ID
     * @param kbId 知识库 ID
     */
    public void deleteDocument(String docId, String kbId) {
        log.info("删除文档及其向量，文档 ID: {}, 知识库 ID: {}", docId, kbId);

        try {
            // 删除向量
            vectorStoreService.deleteDocumentVectors(docId);
            log.info("文档向量删除成功");
        } catch (Exception e) {
            log.error("删除文档失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除知识库及其所有向量
     * @param kbId 知识库 ID
     */
    public void deleteKnowledgeBase(String kbId) {
        log.info("删除知识库及其所有向量，知识库 ID: {}", kbId);

        try {
            // 删除所有向量
            vectorStoreService.deleteKnowledgeBaseVectors(kbId);
            log.info("知识库向量删除成功");
        } catch (Exception e) {
            log.error("删除知识库失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除知识库失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取知识库统计信息
     * @param kbId 知识库 ID
     * @return 统计信息
     */
    public KnowledgeBaseStats getKnowledgeBaseStats(String kbId) {
        java.util.Map<String, Object> stats = vectorStoreService.getKnowledgeBaseStats(kbId);
        KnowledgeBaseStats result = new KnowledgeBaseStats();
        result.setKbId(kbId);
        result.setVectorCount((Integer) stats.getOrDefault("vectorCount", 0));
        result.setEmbeddingModel((String) stats.getOrDefault("embeddingModel", "text-embedding-ada-002"));
        result.setEmbeddingDimension((Integer) stats.getOrDefault("embeddingDimension", 1536));
        return result;
    }

    public boolean hasProcessedDocument(String docId) {
        return vectorStoreService.hasDocumentVectors(docId);
    }
}
