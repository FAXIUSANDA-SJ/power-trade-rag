# ChromaDB & Milvus 集成指南

## 📋 概述

本次集成将 RAG 系统的向量数据库从内存存储升级为专业的向量数据库解决方案：

- **向量数据库**：ChromaDB 或 Milvus（可选）
- **嵌入模型**：OpenAI text-embedding-ada-002（1536 维）
- **框架**：LangChain4j
- **语言模型**：OpenAI GPT-3.5-turbo

---

## 🎯 主要特性

### ✅ 已实现功能

1. **双向量数据库支持**
   - ChromaDB：轻量级，易于部署
   - Milvus：企业级，高性能

2. **OpenAI 嵌入集成**
   - 使用 text-embedding-ada-002 模型
   - 1536 维向量输出
   - 高质量的语义表示

3. **灵活的配置切换**
   - 通过配置文件轻松切换向量数据库
   - 支持自动创建集合/索引

4. **完整的 RAG 流程**
   - 文档预处理和分块
   - 向量化存储
   - 相似度检索
   - 答案生成

---

## 📦 依赖配置

### Maven 依赖（pom.xml）

```xml
<!-- LangChain4j OpenAI 集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- LangChain4j ChromaDB 集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-chroma</artifactId>
    <version>${langchain4j.version}</version>
</dependency>

<!-- LangChain4j Milvus 集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

---

## ⚙️ 配置说明

### application.yml 配置

```yaml
# OpenAI 配置
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY:sk-xxxxxxxxxxxxxxxxxxxxxxxx}
    embedding-model-name: text-embedding-ada-002
    language-model-name: gpt-3.5-turbo
    base-url: https://api.openai.com/v1

# RAG 配置
rag:
  # 向量数据库类型：chroma, milvus
  vector-database-type: chroma
  
  # ChromaDB 配置
  chromadb:
    host: localhost
    port: 8000
    collection-name: power-trade-knowledge
  
  # Milvus 配置
  milvus:
    host: localhost
    port: 19530
    username: 
    password: 
    collection-name: power_trade_knowledge
    database: default
  
  # 通用配置
  vector-store:
    default-type: chroma
    auto-create-collection: true
  
  # 文档处理配置
  document:
    chunk-size: 300
    chunk-overlap: 30
    embedding-dimension: 1536
```

---

## 🚀 快速开始

### 1. 启动向量数据库

#### 使用 Docker Compose（推荐）

```bash
# 启动 ChromaDB 和 Milvus
docker-compose -f docker-compose-vector-db.yml up -d

# 查看服务状态
docker-compose -f docker-compose-vector-db.yml ps

# 停止服务
docker-compose -f docker-compose-vector-db.yml down
```

#### 单独启动 ChromaDB

```bash
docker run -d -p 8000:8000 --name chromadb \
  -v $(pwd)/data/chromadb:/chroma/chroma \
  chromadb/chroma:latest
```

#### 单独启动 Milvus

```bash
# Milvus 需要 etcd 和 minio 依赖，建议使用 docker-compose
```

### 2. 配置 API Key

设置环境变量或修改 application.yml：

```bash
# Windows PowerShell
$env:OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxx"

# Linux/Mac
export OPENAI_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxx"
```

### 3. 启动应用

```bash
cd power-trade-rag-api
mvn spring-boot:run
```

---

## 🔧 核心组件

### 1. 向量数据库配置类

**VectorDatabaseConfig.java**

```java
@Configuration
public class VectorDatabaseConfig {
    
    @Bean
    @ConditionalOnProperty(name = "rag.vector-database-type", havingValue = "chroma")
    public EmbeddingStore embeddingStoreChroma(EmbeddingModel embeddingModel) {
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://" + chromaHost + ":" + chromaPort)
                .collectionName(chromaCollectionName)
                .embeddingModel(embeddingModel)
                .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "rag.vector-database-type", havingValue = "milvus")
    public EmbeddingStore embeddingStoreMilvus(EmbeddingModel embeddingModel) {
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollectionName)
                .embeddingModel(embeddingModel)
                .build();
    }
}
```

### 2. OpenAI 嵌入配置

**OpenAiConfig.java**

```java
@Configuration
public class OpenAiConfig {
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)  // text-embedding-ada-002
                .baseUrl(baseUrl)
                .build();
    }
    
    @Bean
    public OpenAiLanguageModel languageModel() {
        return OpenAiLanguageModel.builder()
                .apiKey(apiKey)
                .modelName(languageModelName)  // gpt-3.5-turbo
                .baseUrl(baseUrl)
                .build();
    }
}
```

### 3. 向量存储服务

**VectorStoreService.java**

主要功能：
- ✅ 文本片段向量化
- ✅ 批量向量添加
- ✅ 相似度搜索
- ✅ 按知识库过滤
- ✅ 向量删除
- ✅ 统计信息

---

## 📊 架构对比

### ChromaDB vs Milvus

| 特性 | ChromaDB | Milvus |
|------|----------|--------|
| **定位** | 轻量级向量数据库 | 企业级向量数据库 |
| **部署难度** | ⭐ 简单 | ⭐⭐⭐ 中等 |
| **性能** | ⭐⭐ 良好 | ⭐⭐⭐⭐ 优秀 |
| **扩展性** | ⭐⭐ 一般 | ⭐⭐⭐⭐ 强大 |
| **资源占用** | ⭐ 低 | ⭐⭐⭐ 较高 |
| **适用场景** | 原型开发、小规模应用 | 生产环境、大规模应用 |

### 嵌入模型对比

| 模型 | 维度 | 性能 | 成本 |
|------|------|------|------|
| text-embedding-ada-002 | 1536 | ⭐⭐⭐⭐⭐ | 付费 |
| text-embedding-3-small | 1536 | ⭐⭐⭐⭐ | 付费（较低） |
| text-embedding-3-large | 3072 | ⭐⭐⭐⭐⭐ | 付费（较高） |

---

## 🔍 使用示例

### 1. 文档上传和向量化

```java
@Autowired
private RagCoreService ragCoreService;

@PostMapping("/upload")
public DocumentInfo uploadDocument(@RequestParam("file") MultipartFile file,
                                  @RequestParam("kbId") String kbId) {
    // 1. 保存文档元数据
    String docId = "DOC" + System.currentTimeMillis();
    DocumentInfo docInfo = documentService.saveDocument(file, kbId, docId);
    
    // 2. 向量化处理
    int segmentCount = ragCoreService.processAndStoreDocument(file, kbId, docId);
    docInfo.setSegmentCount(segmentCount);
    
    return docInfo;
}
```

### 2. 智能问答

```java
@Autowired
private RagCoreService ragCoreService;

@PostMapping("/chat")
public ChatResponse chat(@RequestBody ChatRequest request) {
    // 执行 RAG 问答流程
    return ragCoreService.chat(request);
}
```

### 3. 删除文档

```java
@Autowired
private RagCoreService ragCoreService;

@DeleteMapping("/{docId}")
public void deleteDocument(@PathVariable String docId, 
                          @RequestParam String kbId) {
    ragCoreService.deleteDocument(docId, kbId);
}
```

---

## 🎛️ 切换向量数据库

### 从 ChromaDB 切换到 Milvus

只需修改 application.yml：

```yaml
rag:
  # 修改向量数据库类型
  vector-database-type: milvus  # 从 chroma 改为 milvus
  
  chromadb:
    host: localhost
    port: 8000
  
  milvus:
    host: localhost
    port: 19530
    collection-name: power_trade_knowledge
```

重启应用即可生效！

---

## 📈 性能优化建议

### 1. 向量数据库配置

**ChromaDB：**
- 使用持久化存储（挂载 volume）
- 调整 batch size 以提高吞吐量

**Milvus：**
- 配置适当的索引类型（IVF_FLAT、HNSW）
- 调整搜索参数（nprobe、efConstruction）

### 2. 嵌入优化

- **批量处理**：批量向量化减少 API 调用次数
- **缓存机制**：缓存常用查询的向量结果
- **分块策略**：优化文档分块大小和重叠度

### 3. 检索优化

- **预过滤**：先按知识库过滤再检索
- **相似度阈值**：设置合理的最低相似度分数
- **结果数量**：根据实际需求调整返回结果数

---

## 🔒 安全建议

1. **API Key 管理**
   - 使用环境变量存储 API Key
   - 不要将 API Key 提交到代码仓库
   - 定期轮换 API Key

2. **数据库访问**
   - Milvus 配置用户名密码
   - 限制数据库网络访问
   - 使用 HTTPS 加密通信

3. **数据保护**
   - 定期备份向量数据
   - 实施访问控制
   - 敏感数据脱敏处理

---

## 🐛 常见问题

### Q1: 如何选择合适的向量数据库？

**A:** 
- **开发测试**：使用 ChromaDB，部署简单
- **生产环境**：使用 Milvus，性能更好
- **超大规模**：考虑 Milvus 集群模式

### Q2: text-embedding-ada-002 的成本如何？

**A:**
- 价格：$0.0001 / 1K tokens
- 1000 个中文字符 ≈ 500 tokens ≈ $0.00005
- 建议：批量处理、缓存结果

### Q3: 如何迁移现有向量数据？

**A:**
1. 导出旧向量数据
2. 重新向量化（使用新嵌入模型）
3. 导入新向量数据库

### Q4: 向量数据库启动失败？

**A:**
- 检查 Docker 端口是否被占用
- 查看 Docker 日志：`docker logs <container_name>`
- 确保有足够的磁盘空间

---

## 📚 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [ChromaDB 文档](https://docs.trychroma.com/)
- [Milvus 文档](https://milvus.io/docs)
- [OpenAI Embedding API](https://platform.openai.com/docs/guides/embeddings)

---

## 📝 更新日志

### 2026-05-04
- ✅ 集成 ChromaDB 向量数据库
- ✅ 集成 Milvus 向量数据库
- ✅ 使用 OpenAI text-embedding-ada-002 嵌入模型
- ✅ 支持向量数据库动态切换
- ✅ 完善向量存储服务
- ✅ 创建 Docker Compose 部署配置

---

**集成完成！** 🎉

现在您的 RAG 系统已支持 ChromaDB 和 Milvus 两种专业向量数据库，并使用 OpenAI 的高质量嵌入模型。
