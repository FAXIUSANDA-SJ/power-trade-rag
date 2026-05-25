package com.powertrade.core.model;

import java.io.Serializable;

public class KnowledgeBaseStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private String kbId;
    private Integer vectorCount;
    private String embeddingModel;
    private Integer embeddingDimension;

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public Integer getVectorCount() {
        return vectorCount;
    }

    public void setVectorCount(Integer vectorCount) {
        this.vectorCount = vectorCount;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
}
