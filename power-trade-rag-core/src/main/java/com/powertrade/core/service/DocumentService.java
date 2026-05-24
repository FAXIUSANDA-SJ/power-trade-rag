package com.powertrade.core.service;

import com.powertrade.common.util.FileUtil;
import com.powertrade.core.model.DocumentInfo;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final Tika tika = new Tika();

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
            String filePath = FileUtil.saveFile(fileBytes, originalFilename);
            docInfo.setFilePath(filePath);
            
            // 提取文本内容
            extractText(filePath);
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
            String filePath = FileUtil.saveFile(text.getBytes(), "text_" + docId + ".txt");
            docInfo.setFilePath(filePath);
            
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
            String filePath = FileUtil.saveFile(fileBytes, originalFilename);
            docInfo.setFilePath(filePath);
            
            // 提取文本内容
            extractText(filePath);
            saveDocumentMetadata(docInfo);
            
            return docInfo;
        } catch (Exception e) {
            throw new RuntimeException("文档处理失败", e);
        }
    }

    /**
     * 从文件路径提取文本（保留兼容性）
     * @deprecated 使用 {@link #extractText(String)} 代替
     */
    public String extractText(String filePath) throws IOException, TikaException {
        File file = new File(filePath);
        return tika.parseToString(file);
    }

    /**
     * 保存文档元数据
     */
    public void saveDocumentMetadata(DocumentInfo docInfo) {
        System.out.println("保存文档元数据：" + docInfo.getDocId());
    }

    /**
     * 获取文档列表
     */
    public List<DocumentInfo> getDocumentList(String kbId, Integer page, Integer size) {
        List<DocumentInfo> list = new ArrayList<>();
        DocumentInfo doc = new DocumentInfo();
        doc.setDocId("DOC001");
        doc.setTitle("电力中长期交易规则");
        doc.setFileName("电力中长期交易规则.pdf");
        doc.setDocType("POLICY");
        doc.setStatus(1);
        list.add(doc);
        return list;
    }

    /**
     * 删除文档
     */
    public boolean deleteDocument(String docId) {
        System.out.println("删除文档：" + docId);
        // 同时删除物理文件
        DocumentInfo doc = getDocumentList(null, 1, 10).stream()
            .filter(d -> d.getDocId().equals(docId))
            .findFirst()
            .orElse(null);
        if (doc != null && doc.getFilePath() != null) {
            FileUtil.deleteFile(doc.getFilePath());
        }
        return true;
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
}
