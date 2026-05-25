package com.powertrade.core.model;

import java.io.Serializable;

public class RetrievalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configKey;
    private Integer versionNo;
    private Integer topK;
    private Double minScore;
    private String searchMode;
    private Boolean rerankEnabled;
    private Integer maxReferenceCount;
    private String updatedAt;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public Boolean getRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(Boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public Integer getMaxReferenceCount() {
        return maxReferenceCount;
    }

    public void setMaxReferenceCount(Integer maxReferenceCount) {
        this.maxReferenceCount = maxReferenceCount;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
