package com.powertrade.core.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档向量化服务
 * 负责文档解析、分块、向量化和存储
 */
@Service
public class DocumentVectorizationService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    private final DocumentParser parser = new ApacheTikaDocumentParser();
    private final DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

    /**
     * 处理上传的文档
     * @param file 上传的文件
     * @param kbId 知识库 ID
     * @param docId 文档 ID
     * @return 处理后的文本片段数量
     */
    public int processAndVectorizeDocument(MultipartFile file, String kbId, String docId) throws IOException {
        // 1. 解析文档
        Document document = parser.parse(file.getInputStream());
        
        // 2. 添加元数据
        document.metadata().put("docId", docId);
        document.metadata().put("kbId", kbId);
        document.metadata().put("fileName", file.getOriginalFilename());
        
        // 3. 分割文档
        List<TextSegment> segments = splitter.split(document);
        
        // 4. 向量化并存储
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        
        return segments.size();
    }

    /**
     * 处理文本内容
     * @param text 文本内容
     * @param kbId 知识库 ID
     * @param docId 文档 ID
     * @return 处理后的文本片段数量
     */
    public int processAndVectorizeText(String text, String kbId, String docId) {
        // 1. 创建文档
        Document document = Document.from(text);
        document.metadata().put("docId", docId);
        document.metadata().put("kbId", kbId);
        
        // 2. 分割文档
        List<TextSegment> segments = splitter.split(document);
        
        // 3. 向量化并存储
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        
        return segments.size();
    }

    /**
     * 搜索相关文档
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @param kbId 知识库 ID（可选）
     * @return 相关的文本片段
     */
    public List<EmbeddingMatch<TextSegment>> searchRelevant(String query, int maxResults, String kbId) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        if (kbId != null && !kbId.isEmpty()) {
            // 按知识库过滤
            return embeddingStore.search(queryEmbedding, maxResults, 
                match -> kbId.equals(match.embedding().metadata().get("kbId")));
        } else {
            return embeddingStore.search(queryEmbedding, maxResults);
        }
    }

    /**
     * 删除文档的向量
     * @param docId 文档 ID
     */
    public void removeDocumentVectors(String docId) {
        // 内存存储不支持删除，需要重新构建
        // 生产环境应使用支持删除的向量数据库
        System.out.println("删除文档向量：" + docId);
    }

    /**
     * 获取向量存储中的文档数量
     * @return 文档片段数量
     */
    public int getVectorCount() {
        // 内存存储需要自己实现计数
        return 0;
    }
}
