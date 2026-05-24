package com.powertrade.core.service;

import com.powertrade.core.model.ChatRequest;
import com.powertrade.core.model.ChatResponse;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RAG 核心服务
 * 基于 LangChain4j 实现检索增强生成
 */
@Service
public class RagService {

    @Autowired
    private LanguageModel languageModel;

    @Autowired
    private DocumentVectorizationService vectorizationService;

    /**
     * 智能问答
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        
        try {
            // 生成会话 ID
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            String kbId = request.getKbId();
            String query = request.getQuery();
            
            // 1. 检索相关文档
            List<EmbeddingMatch<TextSegment>> matches = vectorizationService.searchRelevant(query, 5, kbId);
            
            // 2. 构建上下文
            List<String> references = new ArrayList<>();
            StringBuilder contextBuilder = new StringBuilder();
            
            if (!matches.isEmpty()) {
                contextBuilder.append("参考以下电力交易相关知识：\n\n");
                for (int i = 0; i < matches.size(); i++) {
                    EmbeddingMatch<TextSegment> match = matches.get(i);
                    TextSegment segment = match.embedded();
                    
                    String docId = segment.metadata().getString("docId");
                    String fileName = segment.metadata().getString("fileName");
                    
                    contextBuilder.append("【参考资料").append(i + 1).append("】\n");
                    if (fileName != null) {
                        contextBuilder.append("来源：").append(fileName).append("\n");
                    }
                    contextBuilder.append(segment.text()).append("\n\n");
                    
                    if (docId != null) {
                        references.add(docId);
                    }
                }
            }
            
            String context = contextBuilder.toString();
            
            // 3. 构建提示词
            String prompt = buildPrompt(query, context);
            
            // 4. 生成回答
            String answer = languageModel.generate(prompt).content();
            
            // 5. 构建响应
            response.setAnswer(answer);
            response.setSessionId(sessionId);
            response.setReferences(references);
            response.setCode(200);
            response.setMessage("success");
            
        } catch (Exception e) {
            response.setCode(500);
            response.setMessage("查询失败：" + e.getMessage());
            response.setAnswer("抱歉，系统暂时无法处理您的请求。错误信息：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String query, String context) {
        return "您是一位电力交易领域的专业助手，名叫\"小电\"。请用友好、专业的语气回答问题。\n\n" +
               context +
               "用户问题：" + query + "\n\n" +
               "请根据上述参考资料，用通俗易懂的语言回答用户的问题。如果参考资料中没有相关信息，请如实告知。";
    }
    
    /**
     * 向知识库添加文档
     * @param docId 文档 ID
     * @param content 文档内容
     * @param kbId 知识库 ID
     * @return 处理的文本片段数量
     */
    public int addDocumentToKnowledge(String docId, String content, String kbId) {
        return vectorizationService.processAndVectorizeText(content, kbId, docId);
    }
    
    /**
     * 从知识库删除文档
     * @param docId 文档 ID
     */
    public void removeDocumentFromKnowledge(String docId) {
        vectorizationService.removeDocumentVectors(docId);
    }
    
    /**
     * 获取知识库统计信息
     * @return 向量数量
     */
    public int getKnowledgeBaseStats() {
        return vectorizationService.getVectorCount();
    }
}
