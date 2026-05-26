package com.powertrade.core.service.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 会话记忆服务
 */
@Service
public class ConversationMemoryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, ConcurrentLinkedDeque<ConversationTurn>> sessionMemory = new ConcurrentHashMap<>();

    @Autowired
    private PromptConfigService promptConfigService;

    public String getOrCreateSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        return UUID.randomUUID().toString();
    }

    public List<ConversationTurn> getRecentHistory(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return Collections.emptyList();
        }

        ConcurrentLinkedDeque<ConversationTurn> turns = sessionMemory.get(sessionId);
        if (turns == null || turns.isEmpty()) {
            return Collections.emptyList();
        }

        int memoryRounds = promptConfigService.getConfig().getMemoryRounds();
        List<ConversationTurn> history = new ArrayList<>(turns);
        if (history.size() <= memoryRounds) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - memoryRounds, history.size()));
    }

    public void appendConversation(String sessionId, String userMessage, String assistantMessage) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(userMessage) || !StringUtils.hasText(assistantMessage)) {
            return;
        }

        ConcurrentLinkedDeque<ConversationTurn> turns = sessionMemory.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>());
        turns.addLast(new ConversationTurn(userMessage.trim(), assistantMessage.trim(), LocalDateTime.now().format(TIME_FORMATTER)));

        int memoryRounds = promptConfigService.getConfig().getMemoryRounds();
        while (turns.size() > memoryRounds) {
            turns.pollFirst();
        }
    }

    public void clearSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        sessionMemory.remove(sessionId);
    }

    /**
     * 单轮对话
     */
    public static class ConversationTurn implements Serializable {

        private static final long serialVersionUID = 1L;

        private String userMessage;
        private String assistantMessage;
        private String timestamp;

        public ConversationTurn() {
        }

        public ConversationTurn(String userMessage, String assistantMessage, String timestamp) {
            this.userMessage = userMessage;
            this.assistantMessage = assistantMessage;
            this.timestamp = timestamp;
        }

        public String getUserMessage() {
            return userMessage;
        }

        public void setUserMessage(String userMessage) {
            this.userMessage = userMessage;
        }

        public String getAssistantMessage() {
            return assistantMessage;
        }

        public void setAssistantMessage(String assistantMessage) {
            this.assistantMessage = assistantMessage;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}
