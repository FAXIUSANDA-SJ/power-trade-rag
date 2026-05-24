package com.powertrade.core.service.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储管理服务
 * 支持 ChromaDB 和 Milvus 向量数据库
 * 负责向量化、存储、索引和检索
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    // 知识库索引映射
    private final Map<String, Set<String>> knowledgeBaseIndex = new ConcurrentHashMap<>();

    // 文档 ID 到向量 ID 的映射
    private final Map<String, List<String>> documentVectorMap = new ConcurrentHashMap<>();

    /**
     * 向量化并添加文本片段
     * @param segment 文本片段
     * @param kbId 知识库 ID
     * @return 向量 ID
     */
    public String addTextSegment(TextSegment segment, String kbId) {
        try {
            // 1. 生成嵌入向量（使用 OpenAI text-embedding-ada-002）
            Embedding embedding = embeddingModel.embed(segment).content();
            
            // 2. 添加到向量存储
            String vectorId = embeddingStore.add(embedding, segment);
            
            // 3. 更新索引
            if (kbId != null && !kbId.isEmpty()) {
                knowledgeBaseIndex.computeIfAbsent(kbId, k -> ConcurrentHashMap.newKeySet()).add(vectorId);
                
                // 记录文档 ID 到向量 ID 的映射
                String docId = segment.metadata().getString("docId");
                if (docId != null) {
                    documentVectorMap.computeIfAbsent(docId, k -> new ArrayList<>()).add(vectorId);
                }
            }
            
            log.debug("文本片段已向量化并存储，向量 ID: {}", vectorId);
            return vectorId;
            
        } catch (Exception e) {
            log.error("向量化失败：{}", e.getMessage(), e);
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量添加文本片段
     * @param segments 文本片段列表
     * @param kbId 知识库 ID
     * @return 向量 ID 列表
     */
    public List<String> addTextSegments(List<TextSegment> segments, String kbId) {
        List<String> vectorIds = new ArrayList<>();
        
        for (TextSegment segment : segments) {
            String vectorId = addTextSegment(segment, kbId);
            vectorIds.add(vectorId);
        }
        
        log.info("批量添加 {} 个文本片段，生成 {} 个向量", segments.size(), vectorIds.size());
        return vectorIds;
    }

    /**
     * 执行相似度搜索
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 匹配的向量
     */
    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        try {
            // 1. 向量化查询
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .build();
            
            // 3. 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            return searchResult.matches();
            
        } catch (Exception e) {
            log.error("相似度搜索失败：{}", e.getMessage(), e);
            throw new RuntimeException("搜索失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按知识库过滤的相似度搜索
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param kbId 知识库 ID
     * @return 匹配的向量
     */
    public List<EmbeddingMatch<TextSegment>> searchByKnowledgeBase(String query, int maxResults, String kbId) {
        try {
            // 1. 向量化查询
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            // 2. 构建搜索请求（获取更多结果用于过滤）
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults * 3)
                    .build();
            
            // 3. 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();
            
            // 4. 按知识库过滤
            if (kbId != null && !kbId.isEmpty()) {
                Set<String> kbVectors = knowledgeBaseIndex.get(kbId);
                if (kbVectors != null) {
                    matches = matches.stream()
                            .filter(match -> kbVectors.contains(match.embeddingId()))
                            .limit(maxResults)
                            .toList();
                }
            } else {
                matches = matches.stream().limit(maxResults).toList();
            }
            
            return matches;
            
        } catch (Exception e) {
            log.error("按知识库搜索失败：{}", e.getMessage(), e);
            throw new RuntimeException("搜索失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除文档的向量
     * @param docId 文档 ID
     */
    public void deleteDocumentVectors(String docId) {
        try {
            List<String> vectorIds = documentVectorMap.remove(docId);
            if (vectorIds != null && !vectorIds.isEmpty()) {
                // 从向量存储中删除
                for (String vectorId : vectorIds) {
                    embeddingStore.remove(vectorId);
                    
                    // 从知识库索引中移除
                    knowledgeBaseIndex.values().forEach(ids -> ids.remove(vectorId));
                }
                
                log.info("删除文档 {} 的 {} 个向量", docId, vectorIds.size());
            }
        } catch (Exception e) {
            log.error("删除向量失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除向量失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除知识库的所有向量
     * @param kbId 知识库 ID
     */
    public void deleteKnowledgeBaseVectors(String kbId) {
        try {
            Set<String> vectorIds = knowledgeBaseIndex.remove(kbId);
            if (vectorIds != null && !vectorIds.isEmpty()) {
                for (String vectorId : vectorIds) {
                    embeddingStore.remove(vectorId);
                }
                
                log.info("删除知识库 {} 的 {} 个向量", kbId, vectorIds.size());
            }
        } catch (Exception e) {
            log.error("删除知识库向量失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除向量失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取知识库统计信息
     * @param kbId 知识库 ID
     * @return 统计信息
     */
    public Map<String, Object> getKnowledgeBaseStats(String kbId) {
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> vectorIds = knowledgeBaseIndex.get(kbId);
        int vectorCount = vectorIds != null ? vectorIds.size() : 0;
        
        stats.put("vectorCount", vectorCount);
        stats.put("embeddingModel", "text-embedding-ada-002");
        stats.put("embeddingDimension", 1536);
        
        return stats;
    }

    /**
     * 清空所有向量数据
     */
    public void clearAll() {
        try {
            knowledgeBaseIndex.clear();
            documentVectorMap.clear();
            log.info("已清空所有向量数据");
        } catch (Exception e) {
            log.error("清空向量数据失败：{}", e.getMessage(), e);
            throw new RuntimeException("清空数据失败：" + e.getMessage(), e);
        }
    }
}
