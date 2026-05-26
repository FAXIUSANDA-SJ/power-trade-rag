package com.powertrade.core.service.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档预处理服务
 * 负责文档解析、文本清洗、智能分块
 */
@Service
public class DocumentPreprocessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreprocessingService.class);

    private final DocumentParser parser = new ApacheTikaDocumentParser();
    @Value("${rag.document.chunk-size:300}")
    private int chunkSize;

    @Value("${rag.document.chunk-overlap:30}")
    private int chunkOverlap;

    @Value("${rag.document.ocr-enabled:false}")
    private boolean ocrEnabled;

    @Autowired
    private OcrService ocrService;

    /**
     * 解析文档并提取文本
     * @param file 上传的文件
     * @return 解析后的文档对象
     */
    public Document parseDocument(MultipartFile file) throws IOException {
        log.info("开始解析文档：{}", file.getOriginalFilename());
        
        try (InputStream inputStream = file.getInputStream()) {
            Document document = parser.parse(inputStream);

            if (shouldFallbackToOcr(file, document.text())) {
                String ocrText = ocrService.extractText(file);
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    document = Document.from(ocrText);
                }
            }
            
            // 添加元数据
            document.metadata().put("fileName", file.getOriginalFilename());
            document.metadata().put("fileSize", file.getSize());
            document.metadata().put("contentType", file.getContentType());
            
            log.info("文档解析完成，文本长度：{} 字符", document.text().length());
            
            return document;
        } catch (Exception e) {
            log.error("文档解析失败：{}", e.getMessage(), e);
            throw new IOException("文档解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从输入流解析文档
     */
    public Document parseDocument(InputStream inputStream, String fileName) throws IOException {
        try {
            Document document = parser.parse(inputStream);
            document.metadata().put("fileName", fileName);
            return document;
        } catch (Exception e) {
            log.error("文档解析失败：{}", e.getMessage(), e);
            throw new IOException("文档解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解析纯文本
     */
    public Document parseText(String text, String docId, String kbId) {
        Document document = Document.from(text);
        document.metadata().put("docId", docId);
        document.metadata().put("kbId", kbId);
        document.metadata().put("sourceType", "text");
        return document;
    }

    /**
     * 清洗文本内容
     * @param text 原始文本
     * @return 清洗后的文本
     */
    public String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String cleaned = text;

        // 1. 移除多余的空格和换行
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 2. 移除特殊字符（保留中文、英文、数字、标点）
        cleaned = cleaned.replaceAll("[^\\u4e00-\\u9fa5\\w\\s\\p{Punct}]", "");

        // 3. 移除 PDF 转换产生的乱码
        cleaned = removeGarbledText(cleaned);

        // 4. 标准化空格
        cleaned = cleaned.replaceAll(" +", " ");
        cleaned = cleaned.trim();

        log.debug("文本清洗完成，原始长度：{}，清洗后长度：{}", text.length(), cleaned.length());

        return cleaned;
    }

    /**
     * 移除乱码文本
     */
    private String removeGarbledText(String text) {
        // 移除常见的 PDF 转换乱码模式
        Pattern pattern = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F]");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("");
    }

    private boolean shouldFallbackToOcr(MultipartFile file, String text) {
        if (!ocrEnabled) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        boolean imageLike = lowerFileName.endsWith(".png")
                || lowerFileName.endsWith(".jpg")
                || lowerFileName.endsWith(".jpeg")
                || lowerFileName.endsWith(".webp");
        boolean scannedPdfLike = lowerFileName.endsWith(".pdf") && (text == null || text.trim().length() < 20);
        return imageLike || scannedPdfLike;
    }

    /**
     * 分割文档为文本片段
     * @param document 文档对象
     * @return 文本片段列表
     */
    public List<TextSegment> splitDocument(Document document) {
        log.info("开始分割文档，使用分块大小：{}，重叠：{}", chunkSize, chunkOverlap);

        // 使用递归分块器
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
        List<TextSegment> segments = documentSplitter.split(document);

        log.info("文档分割完成，共 {} 个片段", segments.size());

        return segments;
    }

    /**
     * 为文本片段添加元数据
     * @param segments 文本片段列表
     * @param docId 文档 ID
     * @param kbId 知识库 ID
     * @param fileName 文件名
     */
    public void addMetadataToSegments(List<TextSegment> segments, String docId, String kbId, String fileName) {
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata().put("docId", docId);
            segment.metadata().put("kbId", kbId);
            segment.metadata().put("fileName", fileName);
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("totalSegments", String.valueOf(segments.size()));
        }
    }

    /**
     * 完整的文档预处理流程
     * @param file 上传的文件
     * @param docId 文档 ID
     * @param kbId 知识库 ID
     * @return 预处理后的文本片段列表
     */
    public List<TextSegment> preprocessDocument(MultipartFile file, String docId, String kbId) throws IOException {
        log.info("开始预处理文档：{}, ID: {}, 知识库：{}", file.getOriginalFilename(), docId, kbId);

        // 1. 解析文档
        Document document = parseDocument(file);

        // 2. 清洗文本
        String cleanedText = cleanText(document.text());
        Document cleanedDocument = Document.from(cleanedText);
        
        // 复制元数据
        document.metadata().asMap().forEach(cleanedDocument.metadata()::put);

        // 3. 分割文档
        List<TextSegment> segments = splitDocument(cleanedDocument);

        // 4. 添加元数据
        addMetadataToSegments(segments, docId, kbId, file.getOriginalFilename());

        log.info("文档预处理完成，生成 {} 个文本片段", segments.size());

        return segments;
    }

    /**
     * 预处理纯文本
     */
    public List<TextSegment> preprocessText(String text, String docId, String kbId) {
        log.info("开始预处理文本，ID: {}, 知识库：{}", docId, kbId);

        // 1. 创建文档
        Document document = parseText(text, docId, kbId);

        // 2. 清洗文本
        String cleanedText = cleanText(text);
        Document cleanedDocument = Document.from(cleanedText);
        
        document.metadata().asMap().forEach(cleanedDocument.metadata()::put);

        // 3. 分割文档
        List<TextSegment> segments = splitDocument(cleanedDocument);

        // 4. 添加元数据
        addMetadataToSegments(segments, docId, kbId, "text_input");

        log.info("文本预处理完成，生成 {} 个文本片段", segments.size());

        return segments;
    }

    /**
     * 获取文档统计信息
     */
    public DocumentStats getDocumentStats(Document document) {
        String text = document.text();
        
        DocumentStats stats = new DocumentStats();
        stats.setTotalCharacters(text.length());
        stats.setTotalWords(text.split("\\s+").length);
        stats.setTotalLines(text.split("\\n").length);
        
        // 估算分块数量
        stats.setEstimatedSegments((int) Math.ceil((double) text.length() / chunkSize));
        
        return stats;
    }

    /**
     * 文档统计信息类
     */
    public static class DocumentStats {
        private int totalCharacters;
        private int totalWords;
        private int totalLines;
        private int estimatedSegments;

        // Getters and Setters
        public int getTotalCharacters() { return totalCharacters; }
        public void setTotalCharacters(int totalCharacters) { this.totalCharacters = totalCharacters; }
        public int getTotalWords() { return totalWords; }
        public void setTotalWords(int totalWords) { this.totalWords = totalWords; }
        public int getTotalLines() { return totalLines; }
        public void setTotalLines(int totalLines) { this.totalLines = totalLines; }
        public int getEstimatedSegments() { return estimatedSegments; }
        public void setEstimatedSegments(int estimatedSegments) { this.estimatedSegments = estimatedSegments; }
    }
}
