package com.powertrade.core.model;

import java.io.Serializable;

public class KnowledgeBaseConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String kbId;
    private Integer versionNo;
    private String vectorModel;
    private String parseStrategy;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Boolean ocrEnabled;
    private String updatedAt;

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getVectorModel() {
        return vectorModel;
    }

    public void setVectorModel(String vectorModel) {
        this.vectorModel = vectorModel;
    }

    public String getParseStrategy() {
        return parseStrategy;
    }

    public void setParseStrategy(String parseStrategy) {
        this.parseStrategy = parseStrategy;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Boolean getOcrEnabled() {
        return ocrEnabled;
    }

    public void setOcrEnabled(Boolean ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
