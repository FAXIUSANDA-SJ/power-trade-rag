package com.powertrade.core.model;

import java.io.Serializable;

/**
 * 聊天请求模型
 */
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String query;
    private String kbId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }
}
