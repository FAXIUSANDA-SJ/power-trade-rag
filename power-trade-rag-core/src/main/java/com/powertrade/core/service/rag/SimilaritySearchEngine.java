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
import java.util.stream.Collectors;

/**
 * 相似度检索引擎
 * 负责向量检索、过滤、排序和结果处理
 */
@Service
public class SimilaritySearchEngine {

    private static final Logger log = LoggerFactory.getLogger(SimilaritySearchEngine.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    // 默认检索参数
    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final double MIN_SIMILARITY_SCORE = 0.5;

    /**
     * 检索请求构建器
     */
    public static class SearchRequest {
        private String query;
        private int maxResults = DEFAULT_MAX_RESULTS;
        private String kbId;
        private double minScore = MIN_SIMILARITY_SCORE;
        private Map<String, String> filters = new HashMap<>();

        public SearchRequest query(String query) {
            this.query = query;
            return this;
        }

        public SearchRequest maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public SearchRequest kbId(String kbId) {
            this.kbId = kbId;
            return this;
        }

        public SearchRequest minScore(double minScore) {
            this.minScore = minScore;
            return this;
        }

        public SearchRequest filter(String key, String value) {
            this.filters.put(key, value);
            return this;
        }
    }

    /**
     * 检索结果
     */
    public static class SearchResult {
        private List<MatchedDocument> documents;
        private String query;
        private int totalMatches;
        private long searchTimeMs;

        public SearchResult() {
            this.documents = new ArrayList<>();
        }

        // Getters and Setters
        public List<MatchedDocument> getDocuments() { return documents; }
        public void setDocuments(List<MatchedDocument> documents) { this.documents = documents; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public int getTotalMatches() { return totalMatches; }
        public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
        public long getSearchTimeMs() { return searchTimeMs; }
        public void setSearchTimeMs(long searchTimeMs) { this.searchTimeMs = searchTimeMs; }
    }

    /**
     * 匹配的文档
     */
    public static class MatchedDocument {
        private String text;
        private double score;
        private String docId;
        private String kbId;
        private String fileName;
        private int segmentIndex;
        private Map<String, String> metadata;

        public MatchedDocument() {
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getDocId() { return docId; }
        public void setDocId(String docId) { this.docId = docId; }
        public String getKbId() { return kbId; }
        public void setKbId(String kbId) { this.kbId = kbId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public int getSegmentIndex() { return segmentIndex; }
        public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    /**
     * 执行相似度检索
     * @param request 检索请求
     * @return 检索结果
     */
    public SearchResult search(SearchRequest request) {
        log.info("执行相似度检索：{}", request.query);
        
        long startTime = System.currentTimeMillis();
        SearchResult result = new SearchResult();
        result.setQuery(request.query);

        try {
            // 1. 向量化查询
            Embedding queryEmbedding = embeddingModel.embed(request.query).content();

            // 2. 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(request.maxResults * 2) // 获取更多结果用于过滤
                    .build();

            // 3. 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            // 4. 过滤和排序
            List<EmbeddingMatch<TextSegment>> filteredMatches = filterAndSort(matches, request);

            // 5. 转换为结果对象
            List<MatchedDocument> documents = convertToMatchedDocuments(filteredMatches);

            // 6. 设置结果
            result.setDocuments(documents);
            result.setTotalMatches(documents.size());

            long searchTime = System.currentTimeMillis() - startTime;
            result.setSearchTimeMs(searchTime);

            log.info("检索完成，找到 {} 个匹配，耗时 {}ms", documents.size(), searchTime);

            return result;

        } catch (Exception e) {
            log.error("相似度检索失败：{}", e.getMessage(), e);
            throw new RuntimeException("检索失败：" + e.getMessage(), e);
        }
    }

    /**
     * 过滤和排序
     */
    private List<EmbeddingMatch<TextSegment>> filterAndSort(
            List<EmbeddingMatch<TextSegment>> matches, SearchRequest request) {
        
        return matches.stream()
                // 1. 按最低相似度过滤
                .filter(match -> match.score() >= request.minScore)
                // 2. 按知识库 ID 过滤
                .filter(match -> {
                    if (request.kbId == null || request.kbId.isEmpty()) {
                        return true;
                    }
                    TextSegment segment = match.embedded();
                    String segmentKbId = segment.metadata().get("kbId");
                    return request.kbId.equals(segmentKbId);
                })
                // 3. 按自定义过滤器过滤
                .filter(match -> {
                    if (request.filters.isEmpty()) {
                        return true;
                    }
                    TextSegment segment = match.embedded();
                    for (Map.Entry<String, String> filter : request.filters.entrySet()) {
                        String value = segment.metadata().get(filter.getKey());
                        if (value == null || !value.equals(filter.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                // 4. 按相似度排序
                .sorted(Comparator.comparing(EmbeddingMatch::score).reversed())
                // 5. 限制结果数量
                .limit(request.maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 转换为匹配文档对象
     */
    private List<MatchedDocument> convertToMatchedDocuments(
            List<EmbeddingMatch<TextSegment>> matches) {
        
        List<MatchedDocument> documents = new ArrayList<>();
        
        for (EmbeddingMatch<TextSegment> match : matches) {
            MatchedDocument doc = new MatchedDocument();
            TextSegment segment = match.embedded();
            
            doc.setText(segment.text());
            doc.setScore(match.score());
            doc.setDocId(segment.metadata().get("docId"));
            doc.setKbId(segment.metadata().get("kbId"));
            doc.setFileName(segment.metadata().get("fileName"));
            
            String segmentIndex = segment.metadata().get("segmentIndex");
            if (segmentIndex != null) {
                doc.setSegmentIndex(Integer.parseInt(segmentIndex));
            }
            
            // 复制所有元数据
            Map<String, String> metadata = new HashMap<>();
            segment.metadata().asMap().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            doc.setMetadata(metadata);
            
            documents.add(doc);
        }
        
        return documents;
    }

    /**
     * 简单检索方法（向后兼容）
     */
    public List<MatchedDocument> search(String query, int maxResults, String kbId) {
        SearchRequest request = new SearchRequest()
                .query(query)
                .maxResults(maxResults)
                .kbId(kbId);
        
        SearchResult result = search(request);
        return result.getDocuments();
    }

    /**
     * 创建检索请求构建器
     */
    public SearchRequest builder() {
        return new SearchRequest();
    }
}
