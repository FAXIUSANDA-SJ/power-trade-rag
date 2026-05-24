package com.powertrade.core.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenLanguageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 配置类
 * 配置阿里云通义千问模型和向量存储
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.dashscope.api-key:}")
    private String apiKey;

    @Value("${langchain4j.dashscope.embedding-model-name:text-embedding-v2}")
    private String embeddingModelName;

    @Value("${langchain4j.dashscope.language-model-name:qwen-plus}")
    private String languageModelName;

    /**
     * 配置嵌入模型（使用阿里云通义千问）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * 配置语言模型（使用阿里云通义千问）
     */
    @Bean
    public LanguageModel languageModel() {
        return QwenLanguageModel.builder()
                .apiKey(apiKey)
                .modelName(languageModelName)
                .build();
    }

    /**
     * 配置向量存储（使用内存存储，生产环境建议使用 ChromaDB 或 Milvus）
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 初始化配置
     */
    @Bean
    public void initLangChain4j() {
        System.out.println("===================================");
        System.out.println("LangChain4j 配置已加载");
        System.out.println("API Key: " + (apiKey != null && !apiKey.isEmpty() ? "已配置" : "未配置"));
        System.out.println("嵌入模型：" + embeddingModelName);
        System.out.println("语言模型：" + languageModelName);
        System.out.println("===================================");
    }
}
