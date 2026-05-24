package com.powertrade.core.service.rag;

import dev.langchain4j.model.language.LanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${rag.llm.temperature:0.7}")
    private double temperature;

    @Value("${rag.llm.max-tokens:2000}")
    private int maxTokens;

    @Value("${system.name:电力交易智能问答系统}")
    private String systemName;

    /**
     * 提示词构建器
     */
    public static class PromptBuilder {
        private String systemPrompt;
        private String context;
        private String query;
        private List<String> referenceTexts;
        private boolean includeReferences = true;

        public PromptBuilder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
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

        public PromptBuilder references(List<String> referenceTexts) {
            this.referenceTexts = referenceTexts;
            return this;
        }

        public PromptBuilder includeReferences(boolean include) {
            this.includeReferences = include;
            return this;
        }

        public String build() {
            StringBuilder prompt = new StringBuilder();

            // 1. 系统提示
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                prompt.append(systemPrompt).append("\n\n");
            }

            // 2. 上下文（参考资料）
            if (context != null && !context.isEmpty()) {
                prompt.append("参考资料：\n");
                prompt.append(context).append("\n\n");
            }

            // 3. 用户问题
            if (query != null && !query.isEmpty()) {
                prompt.append("用户问题：").append(query).append("\n\n");
            }

            // 4. 指令
            prompt.append("请根据上述参考资料，用通俗易懂的语言回答用户的问题。\n");
            prompt.append("要求：\n");
            prompt.append("1. 答案准确、专业、易懂\n");
            prompt.append("2. 如果参考资料中没有相关信息，请如实告知\n");
            prompt.append("3. 必要时可以引用参考资料中的内容\n");

            return prompt.toString();
        }
    }

    /**
     * 生成答案
     * @param context 上下文（检索到的相关文档）
     * @param query 用户问题
     * @return 生成的答案
     */
    public String generateAnswer(String context, String query) {
        log.info("开始生成答案，问题：\"{}\"", query);

        try {
            // 1. 构建提示词
            String prompt = buildPrompt(context, query);

            // 2. 调用 LLM 生成答案
            String answer = languageModel.generate(prompt);

            // 3. 后处理
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
            // 1. 提取参考文本
            List<String> referenceTexts = matchedDocs.stream()
                    .map(SimilaritySearchEngine.MatchedDocument::getText)
                    .collect(Collectors.toList());

            // 2. 构建上下文
            String context = buildContext(matchedDocs);

            // 3. 构建提示词
            String prompt = buildPromptWithContext(context, query);

            // 4. 调用 LLM 生成答案
            String answer = languageModel.generate(prompt);

            // 5. 后处理
            answer = postProcessAnswer(answer, query);

            // 6. 提取引用信息
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
    private String buildPromptWithContext(String context, String query) {
        return new PromptBuilder()
                .systemPrompt(getDefaultSystemPrompt())
                .context(context)
                .query(query)
                .build();
    }

    /**
     * 构建提示词（简单版本）
     */
    private String buildPrompt(String context, String query) {
        return new PromptBuilder()
                .systemPrompt(getDefaultSystemPrompt())
                .context(context)
                .query(query)
                .build();
    }

    /**
     * 获取默认系统提示
     */
    private String getDefaultSystemPrompt() {
        return "您是一位电力交易领域的专业助手，名叫\"小电\"。\n" +
               "请用友好、专业、通俗易懂的语气回答问题。\n" +
               "您擅长解释复杂的电力交易概念、政策和市场规则。";
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
            return languageModel.generate(customPrompt);
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
