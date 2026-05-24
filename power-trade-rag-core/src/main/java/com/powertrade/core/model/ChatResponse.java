package com.powertrade.core.model;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天响应模型
 */
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String answer;
    private List<String> references;
    private String sessionId;
    private Integer code = 200;
    private String message = "success";

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
