package com.powertrade.core.service.rag;

import com.powertrade.core.model.PromptConfig;
import dev.langchain4j.model.language.LanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 答案生成服务
 * 负责提示词构建、LLM 调用、答案后处理
 */
@Service
public class AnswerGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AnswerGenerationService.class);

    @Autowired
    private LanguageModel languageModel;

    @Autowired
    private PromptConfigService promptConfigService;

    /**
     * 提示词构建器
     */
    public static class PromptBuilder {
        private String systemPrompt;
        private String conversationHistory;
        private String context;
        private String query;

        public PromptBuilder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public PromptBuilder conversationHistory(String conversationHistory) {
            this.conversationHistory = conversationHistory;
            return this;
        }

        public PromptBuilder context(String context) {
            this.context = context;
            return this;
        }

        public PromptBuilder query(String query) {
            this.query = query;
            return this;
        }

        public String build() {
            StringBuilder prompt = new StringBuilder();

            // 1. 系统提示
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                prompt.append(systemPrompt).append("\n\n");
            }

            // 2. 历史对话
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                prompt.append("历史对话：\n");
                prompt.append(conversationHistory).append("\n\n");
            }

            // 3. 上下文（参考资料）
            if (context != null && !context.isEmpty()) {
                prompt.append("参考资料：\n");
                prompt.append(context).append("\n\n");
            }

            // 4. 用户问题
            if (query != null && !query.isEmpty()) {
                prompt.append("用户问题：").append(query).append("\n\n");
            }

            // 5. 指令
            prompt.append("请结合历史对话和参考资料，用通俗易懂的语言回答用户问题。\n");
            prompt.append("要求：\n");
            prompt.append("1. 优先延续当前对话上下文，不要忽略用户之前已说明的信息\n");
            prompt.append("2. 答案准确、专业、易懂，必要时分点说明\n");
            prompt.append("3. 如果参考资料中没有相关信息，请如实告知，不要编造\n");
            prompt.append("4. 必要时给出下一步建议或澄清问题\n");

            return prompt.toString();
        }
    }

    /**
     * 基于参考文档和对话记忆生成答案
     * @param query 用户问题
     * @param matchedDocs 检索到的相关文档
     * @param conversationHistory 会话历史
     * @return 生成的答案
     */
    public String generateAnswer(String query,
                                 List<SimilaritySearchEngine.MatchedDocument> matchedDocs,
                                 List<ConversationMemoryService.ConversationTurn> conversationHistory) {
        log.info("开始生成答案，问题：\"{}\"", query);

        try {
            String context = buildContext(matchedDocs);
            String history = buildConversationHistory(conversationHistory);
            String prompt = buildPromptWithContext(context, query, history);
            String answer = languageModel.generate(prompt).content();
            answer = postProcessAnswer(answer, query);

            log.info("答案生成完成，长度：{} 字符", answer.length());
            return answer;
        } catch (Exception e) {
            log.error("答案生成失败：{}", e.getMessage(), e);
            throw new RuntimeException("答案生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 兼容简单文本上下文的生成接口
     * @param context 上下文（检索到的相关文档）
     * @param query 用户问题
     * @return 生成的答案
     */
    public String generateAnswer(String context, String query) {
        log.info("开始生成答案，问题：\"{}\"", query);

        try {
            String prompt = buildPrompt(context, query);
            String answer = languageModel.generate(prompt).content();
            answer = postProcessAnswer(answer, query);

            log.info("答案生成完成，长度：{} 字符", answer.length());
            return answer;
        } catch (Exception e) {
            log.error("答案生成失败：{}", e.getMessage(), e);
            throw new RuntimeException("答案生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成答案（带引用）
     * @param matchedDocs 匹配的文档列表
     * @param query 用户问题
     * @return 生成的答案和引用
     */
    public AnswerResult generateAnswerWithReferences(List<SimilaritySearchEngine.MatchedDocument> matchedDocs, String query) {
        log.info("开始生成答案（带引用），问题：\"{}\", 参考文档：{} 个", query, matchedDocs.size());

        try {
            // 1. 构建上下文
            String context = buildContext(matchedDocs);

            // 2. 构建提示词
            String prompt = buildPromptWithContext(context, query, "");

            // 3. 调用 LLM 生成答案
            String answer = languageModel.generate(prompt).content();

            // 4. 后处理
            answer = postProcessAnswer(answer, query);

            // 5. 提取引用信息
            List<Reference> references = extractReferences(matchedDocs);

            log.info("答案生成完成，包含 {} 个引用", references.size());

            AnswerResult result = new AnswerResult();
            result.setAnswer(answer);
            result.setReferences(references);
            result.setContext(context);

            return result;

        } catch (Exception e) {
            log.error("答案生成失败：{}", e.getMessage(), e);
            throw new RuntimeException("答案生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 构建上下文
     */
    private String buildContext(List<SimilaritySearchEngine.MatchedDocument> matchedDocs) {
        if (matchedDocs == null || matchedDocs.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        for (int i = 0; i < matchedDocs.size(); i++) {
            SimilaritySearchEngine.MatchedDocument doc = matchedDocs.get(i);
            
            context.append("【参考资料").append(i + 1).append("】\n");
            
            if (doc.getFileName() != null) {
                context.append("来源：").append(doc.getFileName()).append("\n");
            }
            
            context.append(doc.getText()).append("\n\n");
        }

        return context.toString();
    }

    /**
     * 构建提示词
     */
    private String buildPromptWithContext(String context, String query, String conversationHistory) {
        return new PromptBuilder()
                .systemPrompt(getSystemPrompt())
                .conversationHistory(conversationHistory)
                .context(context)
                .query(query)
                .build();
    }

    /**
     * 构建提示词（简单版本）
     */
    private String buildPrompt(String context, String query) {
        return buildPrompt(context, query, "");
    }

    private String buildPrompt(String context, String query, String conversationHistory) {
        return new PromptBuilder()
                .systemPrompt(getSystemPrompt())
                .conversationHistory(conversationHistory)
                .context(context)
                .query(query)
                .build();
    }

    /**
     * 构建会话历史
     */
    private String buildConversationHistory(List<ConversationMemoryService.ConversationTurn> conversationHistory) {
        List<ConversationMemoryService.ConversationTurn> turns =
                conversationHistory == null ? Collections.emptyList() : conversationHistory;
        if (turns.isEmpty()) {
            return "";
        }

        StringBuilder history = new StringBuilder();
        for (int i = 0; i < turns.size(); i++) {
            ConversationMemoryService.ConversationTurn turn = turns.get(i);
            history.append("第").append(i + 1).append("轮对话").append("（").append(turn.getTimestamp()).append("）").append("\n");
            history.append("用户：").append(turn.getUserMessage()).append("\n");
            history.append("助手：").append(turn.getAssistantMessage()).append("\n\n");
        }
        return history.toString().trim();
    }

    /**
     * 获取系统提示
     */
    private String getSystemPrompt() {
        PromptConfig config = promptConfigService.getConfig();
        String assistantName = StringUtils.hasText(config.getAssistantName()) ? config.getAssistantName().trim() : "小电";
        String systemPrompt = StringUtils.hasText(config.getSystemPrompt()) ? config.getSystemPrompt().trim() : "";

        if (systemPrompt.contains(assistantName)) {
            return systemPrompt;
        }
        return "当前助手名称为\"" + assistantName + "\"。\n" + systemPrompt;
    }

    /**
     * 答案后处理
     */
    private String postProcessAnswer(String answer, String query) {
        if (answer == null || answer.trim().isEmpty()) {
            return "抱歉，我暂时无法回答这个问题。";
        }

        String processed = answer.trim();

        // 1. 移除可能的重复开头
        if (processed.startsWith("用户问题：")) {
            int index = processed.indexOf("\n", processed.indexOf("用户问题："));
            if (index != -1) {
                processed = processed.substring(index + 1).trim();
            }
        }

        // 2. 确保答案完整
        if (!processed.endsWith("。") && !processed.endsWith("！") && !processed.endsWith("？")) {
            // 答案不完整，尝试截断或补充
            if (processed.length() > 100) {
                int lastPeriod = processed.lastIndexOf("。");
                if (lastPeriod > 0) {
                    processed = processed.substring(0, lastPeriod + 1);
                }
            }
        }

        // 3. 添加友好的开头（如果合适）
        if (!processed.startsWith("根据") && !processed.startsWith("您好") && !processed.startsWith("关于")) {
            // 可以选择添加，但保持简洁
        }

        return processed;
    }

    /**
     * 提取引用信息
     */
    private List<Reference> extractReferences(List<SimilaritySearchEngine.MatchedDocument> matchedDocs) {
        return matchedDocs.stream()
                .map(doc -> {
                    Reference ref = new Reference();
                    ref.setDocId(doc.getDocId());
                    ref.setFileName(doc.getFileName());
                    ref.setKbId(doc.getKbId());
                    ref.setSegmentIndex(doc.getSegmentIndex());
                    ref.setScore(doc.getScore());
                    return ref;
                })
                .collect(Collectors.toList());
    }

    /**
     * 自定义提示词生成答案
     */
    public String generateWithCustomPrompt(String customPrompt) {
        log.info("使用自定义提示词生成答案");

        try {
            return languageModel.generate(customPrompt).content();
        } catch (Exception e) {
            log.error("答案生成失败：{}", e.getMessage(), e);
            throw new RuntimeException("答案生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 答案结果类
     */
    public static class AnswerResult {
        private String answer;
        private List<Reference> references;
        private String context;

        // Getters and Setters
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public List<Reference> getReferences() { return references; }
        public void setReferences(List<Reference> references) { this.references = references; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }

    /**
     * 引用信息类
     */
    public static class Reference {
        private String docId;
        private String fileName;
        private String kbId;
        private int segmentIndex;
        private double score;

        // Getters and Setters
        public String getDocId() { return docId; }
        public void setDocId(String docId) { this.docId = docId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getKbId() { return kbId; }
        public void setKbId(String kbId) { this.kbId = kbId; }
        public int getSegmentIndex() { return segmentIndex; }
        public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    /**
     * 创建提示词构建器
     */
    public PromptBuilder promptBuilder() {
        return new PromptBuilder();
    }
}
