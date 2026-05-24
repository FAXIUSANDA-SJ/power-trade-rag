package com.powertrade.api.controller;

import com.powertrade.core.model.DocumentInfo;
import com.powertrade.core.service.DocumentService;
import com.powertrade.core.service.rag.RagCoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文档管理控制器
 * 基于完整的 RAG 流程实现文档处理和向量化
 */
@RestController
@RequestMapping("/api/document")
@Api(tags = "文档管理接口")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RagCoreService ragCoreService;

    @PostMapping("/upload")
    @ApiOperation("上传文档并处理")
    public DocumentInfo uploadDocument(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "kbId", required = false, defaultValue = "default") String kbId) {
        try {
            log.info("收到文档上传请求，文件名：{}, 知识库：{}", file.getOriginalFilename(), kbId);

            // 1. 生成文档 ID
            String docId = "DOC" + System.currentTimeMillis();

            // 2. 保存文档元数据
            DocumentInfo docInfo = documentService.saveDocument(file, kbId, docId);

            // 3. 使用 RAG 服务处理并向量化文档
            int segmentCount = ragCoreService.processAndStoreDocument(file, kbId, docId);
            docInfo.setSegmentCount(segmentCount);

            log.info("文档上传完成，ID: {}, 片段数：{}", docId, segmentCount);

            return docInfo;

        } catch (IOException e) {
            log.error("文档上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @PostMapping("/uploadText")
    @ApiOperation("上传纯文本")
    public DocumentInfo uploadText(@RequestBody TextUploadRequest request) {
        try {
            log.info("收到文本上传请求，知识库：{}, 文本长度：{}", request.getKbId(), request.getText().length());

            // 1. 生成文档 ID
            String docId = "DOC" + System.currentTimeMillis();

            // 2. 保存文档元数据
            DocumentInfo docInfo = documentService.saveText(request.getText(), request.getKbId(), docId);

            // 3. 使用 RAG 服务处理并向量化文本
            int segmentCount = ragCoreService.processAndStoreText(
                request.getText(), 
                request.getKbId(), 
                docId
            );
            docInfo.setSegmentCount(segmentCount);

            log.info("文本上传完成，ID: {}, 片段数：{}", docId, segmentCount);

            return docInfo;

        } catch (Exception e) {
            log.error("文本上传失败：{}", e.getMessage(), e);
            throw new RuntimeException("文本上传失败：" + e.getMessage(), e);
        }
    }

    @GetMapping("/list")
    @ApiOperation("获取文档列表")
    public List<DocumentInfo> getDocumentList(
            @RequestParam(value = "kbId", required = false) String kbId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return documentService.getDocumentList(kbId, page, size);
    }

    @DeleteMapping("/{docId}")
    @ApiOperation("删除文档")
    public boolean deleteDocument(@PathVariable String docId) {
        log.info("删除文档：{}", docId);

        try {
            // 1. 删除向量存储中的文档
            ragCoreService.deleteDocument(docId);

            // 2. 删除数据库中的文档记录
            return documentService.deleteDocument(docId);

        } catch (Exception e) {
            log.error("删除文档失败：{}", e.getMessage(), e);
            throw new RuntimeException("删除失败：" + e.getMessage(), e);
        }
    }

    @GetMapping("/stats/{kbId}")
    @ApiOperation("获取知识库统计信息")
    public com.powertrade.core.service.rag.VectorStoreService.KnowledgeBaseStats getKnowledgeBaseStats(
            @PathVariable String kbId) {
        return ragCoreService.getKnowledgeBaseStats(kbId);
    }

    /**
     * 文本上传请求
     */
    public static class TextUploadRequest {
        private String text;
        private String kbId;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getKbId() { return kbId; }
        public void setKbId(String kbId) { this.kbId = kbId; }
    }
}
