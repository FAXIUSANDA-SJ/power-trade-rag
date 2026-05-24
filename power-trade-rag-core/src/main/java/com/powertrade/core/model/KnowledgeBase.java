package com.powertrade.core.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识库模型
 */
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private String kbId;
    private String name;
    private String description;
    private Integer status;
    private Date createTime;

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
