package com.powertrade.core.model;

import java.io.Serializable;
import java.util.Date;

public class DocumentIngestAcceptedResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String docId;
    private String kbId;
    private String title;
    private String fileName;
    private String docType;
    private Long fileSize;
    private String ingestTaskId;
    private String ingestStatus;
    private Integer segmentCount;
    private String message;
    private Date acceptedAt;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getIngestTaskId() {
        return ingestTaskId;
    }

    public void setIngestTaskId(String ingestTaskId) {
        this.ingestTaskId = ingestTaskId;
    }

    public String getIngestStatus() {
        return ingestStatus;
    }

    public void setIngestStatus(String ingestStatus) {
        this.ingestStatus = ingestStatus;
    }

    public Integer getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(Integer segmentCount) {
        this.segmentCount = segmentCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
