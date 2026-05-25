package com.powertrade.core.model;

import java.io.Serializable;

/**
 * 对话助手提示词配置
 */
public class PromptConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String assistantName;
    private String welcomeMessage;
    private String systemPrompt;
    private String fallbackReply;
    private Integer memoryRounds;
    private Integer versionNo;
    private String configKey;
    private String updatedAt;

    public String getAssistantName() {
        return assistantName;
    }

    public void setAssistantName(String assistantName) {
        this.assistantName = assistantName;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getFallbackReply() {
        return fallbackReply;
    }

    public void setFallbackReply(String fallbackReply) {
        this.fallbackReply = fallbackReply;
    }

    public Integer getMemoryRounds() {
        return memoryRounds;
    }

    public void setMemoryRounds(Integer memoryRounds) {
        this.memoryRounds = memoryRounds;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
