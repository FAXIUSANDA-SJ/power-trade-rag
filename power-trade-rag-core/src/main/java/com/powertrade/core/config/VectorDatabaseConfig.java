package com.powertrade.core.config;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量数据库配置
 * 支持 ChromaDB 和 Milvus 两种向量数据库
 */
@Configuration
public class VectorDatabaseConfig {

    @Value("${rag.chromadb.host:localhost}")
    private String chromaHost;

    @Value("${rag.chromadb.port:8000}")
    private int chromaPort;

    @Value("${rag.chromadb.collection-name:power-trade-knowledge}")
    private String chromaCollectionName;

    @Value("${rag.milvus.host:localhost}")
    private String milvusHost;

    @Value("${rag.milvus.port:19530}")
    private int milvusPort;

    @Value("${rag.milvus.username:}")
    private String milvusUsername;

    @Value("${rag.milvus.password:}")
    private String milvusPassword;

    @Value("${rag.milvus.collection-name:power_trade_knowledge}")
    private String milvusCollectionName;

    @Value("${rag.milvus.database:default}")
    private String milvusDatabase;

    @Value("${rag.vector-store.auto-create-collection:true}")
    private boolean autoCreateCollection;

    @Value("${rag.vector-database-type:chroma}")
    private String vectorDatabaseType;

    /**
     * ChromaDB 向量存储
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vector-database-type", havingValue = "chroma", matchIfMissing = true)
    public EmbeddingStore embeddingStoreChroma() {
        System.out.println("===================================");
        System.out.println("初始化 ChromaDB 向量存储");
        System.out.println("地址：" + chromaHost + ":" + chromaPort);
        System.out.println("集合：" + chromaCollectionName);
        System.out.println("===================================");

        return ChromaEmbeddingStore.builder()
                .baseUrl("http://" + chromaHost + ":" + chromaPort)
                .collectionName(chromaCollectionName)
                .build();
    }

    /**
     * Milvus 向量存储
     */
    @Bean
    @ConditionalOnProperty(name = "rag.vector-database-type", havingValue = "milvus")
    public EmbeddingStore embeddingStoreMilvus() {
        System.out.println("===================================");
        System.out.println("初始化 Milvus 向量存储");
        System.out.println("地址：" + milvusHost + ":" + milvusPort);
        System.out.println("集合：" + milvusCollectionName);
        System.out.println("数据库：" + milvusDatabase);
        System.out.println("===================================");

        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollectionName)
                .databaseName(milvusDatabase);

        // 如果配置了用户名密码，则添加认证
        if (milvusUsername != null && !milvusUsername.isEmpty()) {
            builder.username(milvusUsername);
            builder.password(milvusPassword);
        }

        return builder.build();
    }

    /**
     * 获取当前使用的向量数据库类型
     */
    public String getVectorDatabaseType() {
        return vectorDatabaseType;
    }
}
