package com.powertrade.core.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI 模型配置
 * 配置 text-embedding-ada-002 嵌入模型和 GPT 语言模型
 */
@Configuration
@ConditionalOnProperty(name = "rag.ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiConfig {

    @Value("${langchain4j.open-ai.api-key:sk-xxxxxxxxxxxxxxxxxxxxxxxx}")
    private String apiKey;

    @Value("${langchain4j.open-ai.embedding-model-name:text-embedding-ada-002}")
    private String embeddingModelName;

    @Value("${langchain4j.open-ai.language-model-name:gpt-3.5-turbo}")
    private String languageModelName;

    @Value("${langchain4j.open-ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * OpenAI 嵌入模型配置
     * 使用 text-embedding-ada-002，输出维度为 1536
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        System.out.println("===================================");
        System.out.println("初始化 OpenAI Embedding 模型");
        System.out.println("模型：" + embeddingModelName);
        System.out.println("维度：1536");
        System.out.println("===================================");

        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * OpenAI 语言模型配置
     * 用于生成回答
     */
    @Bean
    public OpenAiLanguageModel languageModel() {
        System.out.println("===================================");
        System.out.println("初始化 OpenAI Language Model");
        System.out.println("模型：" + languageModelName);
        System.out.println("===================================");

        return OpenAiLanguageModel.builder()
                .apiKey(apiKey)
                .modelName(languageModelName)
                .baseUrl(baseUrl)
                .tokenizer(new OpenAiTokenizer(languageModelName))
                .build();
    }

    /**
     * 获取嵌入模型名称
     */
    public String getEmbeddingModelName() {
        return embeddingModelName;
    }

    /**
     * 获取语言模型名称
     */
    public String getLanguageModelName() {
        return languageModelName;
    }
}
