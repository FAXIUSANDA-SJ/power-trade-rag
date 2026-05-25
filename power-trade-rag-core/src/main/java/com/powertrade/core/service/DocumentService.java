package com.powertrade.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powertrade.common.util.FileUtil;
import com.powertrade.core.model.DocumentInfo;
import com.powertrade.core.model.IngestTask;
import com.powertrade.core.storage.FileStorageService;
import com.powertrade.core.storage.StoredFile;
import com.powertrade.dal.entity.DocumentEntity;
import com.powertrade.dal.mapper.DocumentMapper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final Tika tika = new Tika();

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private IngestTaskService ingestTaskService;

    /**
     * 处理上传的文档
     * @param file 上传的文件
     * @param kbId 知识库 ID
     * @param docId 文档 ID
     * @return 文档信息
     */
    public DocumentInfo saveDocument(MultipartFile file, String kbId, String docId) {
        DocumentInfo docInfo = new DocumentInfo();
        
        try {
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            
            docInfo.setDocId(docId);
            docInfo.setTitle(originalFilename);
            docInfo.setFileName(originalFilename);
            docInfo.setKbId(kbId);
            docInfo.setFileSize((long) fileBytes.length);
            docInfo.setDocType(getFileType(originalFilename));
            docInfo.setStatus(1);
            
            // 保存文件
            StoredFile storedFile = fileStorageService.save(fileBytes, originalFilename);
            docInfo.setFilePath(storedFile.getAccessPath());
            
            saveDocumentMetadata(docInfo);
            
            return docInfo;
        } catch (Exception e) {
            throw new RuntimeException("文档保存失败", e);
        }
    }

    /**
     * 保存纯文本
     */
    public DocumentInfo saveText(String text, String kbId, String docId) {
        DocumentInfo docInfo = new DocumentInfo();
        
        try {
            docInfo.setDocId(docId);
            docInfo.setTitle("文本_" + System.currentTimeMillis());
            docInfo.setFileName("text_input.txt");
            docInfo.setKbId(kbId);
            docInfo.setFileSize((long) text.length());
            docInfo.setDocType("TEXT");
            docInfo.setStatus(1);
            
            // 保存文本文件
            StoredFile storedFile = fileStorageService.save(text.getBytes(), "text_" + docId + ".txt");
            docInfo.setFilePath(storedFile.getAccessPath());
            
            saveDocumentMetadata(docInfo);
            
            return docInfo;
        } catch (Exception e) {
            throw new RuntimeException("文本保存失败", e);
        }
    }

    /**
     * 处理上传的文档（旧方法，保留兼容性）
     */
    public DocumentInfo processDocument(byte[] fileBytes, String originalFilename, String kbId) {
        DocumentInfo docInfo = new DocumentInfo();
        
        try {
            String docId = UUID.randomUUID().toString();
            docInfo.setDocId(docId);
            docInfo.setTitle(originalFilename);
            docInfo.setFileName(originalFilename);
            docInfo.setFileSize((long) fileBytes.length);
            docInfo.setDocType(getFileType(originalFilename));
            docInfo.setStatus(1);
            
            // 保存文件
            StoredFile storedFile = fileStorageService.save(fileBytes, originalFilename);
            docInfo.setFilePath(storedFile.getAccessPath());
            
            saveDocumentMetadata(docInfo);
            
            return docInfo;
        } catch (Exception e) {
            throw new RuntimeException("文档处理失败", e);
        }
    }

    /**
     * 保存文档元数据
     */
    public void saveDocumentMetadata(DocumentInfo docInfo) {
        DocumentEntity entity = new DocumentEntity();
        entity.setDocId(docInfo.getDocId());
        entity.setTitle(docInfo.getTitle());
        entity.setFileName(docInfo.getFileName());
        entity.setFilePath(docInfo.getFilePath());
        entity.setFileSize(docInfo.getFileSize());
        entity.setDocType(docInfo.getDocType());
        if ("TEXT".equals(docInfo.getDocType())) {
            entity.setContent(readDocumentContent(docInfo.getFilePath()));
        }
        entity.setStatus(docInfo.getStatus());
        entity.setCreator("system");
        documentMapper.insert(entity);
    }

    /**
     * 获取文档列表
     */
    public List<DocumentInfo> getDocumentList(String kbId, String ingestStatus, Integer page, Integer size) {
        LambdaQueryWrapper<DocumentEntity> query = new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getStatus, 1)
                .orderByDesc(DocumentEntity::getCreateTime);
        return documentMapper.selectList(query).stream()
                .map(this::toModel)
                .filter(doc -> kbId == null || kbId.trim().isEmpty() || kbId.equals(doc.getKbId()))
                .filter(doc -> ingestStatus == null || ingestStatus.trim().isEmpty() || ingestStatus.equals(doc.getIngestStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        DocumentEntity entity = documentMapper.selectOne(
                new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getDocId, docId)
        );
        if (entity == null) {
            return false;
        }
        if (entity.getFilePath() != null) {
            fileStorageService.delete(entity.getFilePath());
        }
        entity.setStatus(0);
        return documentMapper.updateById(entity) > 0;
    }

    public DocumentInfo getDocumentByDocId(String docId) {
        DocumentEntity entity = documentMapper.selectOne(
                new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getDocId, docId).last("limit 1")
        );
        return entity == null ? null : toModel(entity);
    }

    public String readDocumentContent(String filePath) {
        try {
            byte[] fileBytes = fileStorageService.read(filePath);
            return tika.parseToString(new java.io.ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new RuntimeException("读取文档内容失败", e);
        }
    }

    public String getStoredContent(String docId) {
        DocumentEntity entity = documentMapper.selectOne(
                new LambdaQueryWrapper<DocumentEntity>().eq(DocumentEntity::getDocId, docId).last("limit 1")
        );
        return entity == null ? null : entity.getContent();
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "UNKNOWN";
        }
        String extension = FileUtil.getFileType(fileName);
        switch (extension.toLowerCase()) {
            case "pdf":
                return "PDF";
            case "doc":
            case "docx":
                return "WORD";
            case "xls":
            case "xlsx":
                return "EXCEL";
            case "txt":
                return "TEXT";
            default:
                return "OTHER";
        }
    }

    private DocumentInfo toModel(DocumentEntity entity) {
        DocumentInfo doc = new DocumentInfo();
        doc.setDocId(entity.getDocId());
        doc.setTitle(entity.getTitle());
        doc.setFileName(entity.getFileName());
        doc.setFilePath(entity.getFilePath());
        doc.setFileSize(entity.getFileSize());
        doc.setDocType(entity.getDocType());
        doc.setStatus(entity.getStatus());
        doc.setCreateTime(entity.getCreateTime());
        IngestTask latestTask = ingestTaskService.getLatestTaskByDocId(entity.getDocId());
        if (latestTask != null) {
            doc.setKbId(latestTask.getKbId());
            doc.setIngestTaskId(latestTask.getTaskId());
            doc.setIngestStatus(latestTask.getStatus());
        }
        return doc;
    }
}
