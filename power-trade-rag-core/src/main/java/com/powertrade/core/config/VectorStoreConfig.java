package com.powertrade.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 * 保留配置项，实际向量存储由 LangChain4jConfig 中的 EmbeddingStore 提供
 */
@Configuration
public class VectorStoreConfig {

    @Value("${rag.chromadb.host:localhost}")
    private String chromaHost;

    @Value("${rag.chromadb.port:8000}")
    private int chromaPort;

    @Value("${rag.vector-store.collection-name:power-trade-knowledge}")
    private String collectionName;

    @Bean
    public void initVectorStoreConfig() {
        System.out.println("===================================");
        System.out.println("向量存储配置已加载");
        System.out.println("ChromaDB 地址：" + chromaHost + ":" + chromaPort);
        System.out.println("集合名称：" + collectionName);
        System.out.println("注：当前使用内存向量存储");
        System.out.println("===================================");
    }
}
