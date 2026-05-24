# LangChain4j + Spring Boot 集成指南

## 📋 概述

本指南详细介绍如何在电力交易智能问答系统中集成 LangChain4j 和 Spring Boot，实现完整的 RAG（检索增强生成）功能。

---

## 🎯 技术架构

### 核心技术栈
- **Spring Boot 2.7.14** - 后端框架
- **LangChain4j 0.31.0** - LLM 应用开发框架
- **阿里云通义千问** - 语言模型和嵌入模型
- **Apache Tika** - 文档解析
- **内存向量存储** - 向量检索（可替换为 ChromaDB/Milvus）

### 架构层次
```
┌─────────────────────────────────────────┐
│          前端层 (Vue3 + Element Plus)    │
├─────────────────────────────────────────┤
│          控制层 (Spring MVC)            │
│  - ChatController                       │
│  - DocumentController                   │
│  - KnowledgeBaseController              │
├─────────────────────────────────────────┤
│          服务层 (Spring Service)        │
│  - RagService                           │
│  - DocumentVectorizationService         │
│  - DocumentService                      │
├─────────────────────────────────────────┤
│       LangChain4j 核心组件              │
│  - EmbeddingModel (通义千问)            │
│  - LanguageModel (通义千问)             │
│  - EmbeddingStore (内存)                │
│  - DocumentParser (Apache Tika)         │
├─────────────────────────────────────────┤
│          数据层 (MySQL + Redis)         │
└─────────────────────────────────────────┘
```

---

## 📦 依赖配置

### 1. 根 pom.xml - 配置版本和镜像

```xml
<properties>
    <langchain4j.version>0.31.0</langchain4j.version>
</properties>

<repositories>
    <repository>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
    </repository>
</repositories>
```

### 2. core 模块 pom.xml - LangChain4j 依赖

```xml
<dependencies>
    <!-- LangChain4j 核心 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j Spring Boot Starter -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-spring-boot-starter</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j DashScope (阿里云通义千问) -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-dashscope</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j 文档解析 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-document-parser-apache-tika</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
    
    <!-- LangChain4j 向量存储 -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-embeddings</artifactId>
        <version>${langchain4j.version}</version>
    </dependency>
</dependencies>
```

---

## ⚙️ 配置说明

### 1. application.yml 配置

```yaml
# LangChain4j 配置（使用阿里云通义千问）
langchain4j:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:sk-xxxxxxxx}  # 阿里云 API Key
    embedding-model-name: text-embedding-v2    # 嵌入模型
    language-model-name: qwen-plus             # 语言模型

# RAG 配置
rag:
  document:
    chunk-size: 300        # 文本分块大小
    chunk-overlap: 30      # 分块重叠
    upload-dir: ./data/docs # 上传目录
```

### 2. 获取阿里云 API Key

1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 注册/登录阿里云账号
3. 开通 DashScope 服务
4. 创建 API Key
5. 将 API Key 填入配置文件或设置环境变量

**环境变量方式：**
```bash
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

---

## 🔧 核心组件

### 1. LangChain4jConfig - 配置类

```java
@Configuration
public class LangChain4jConfig {
    
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v2")
                .build();
    }
    
    @Bean
    public LanguageModel languageModel() {
        return QwenLanguageModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-plus")
                .build();
    }
    
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
```

**功能说明：**
- 配置嵌入模型（文本向量化）
- 配置语言模型（文本生成）
- 配置向量存储（内存/数据库）

### 2. DocumentVectorizationService - 文档向量化

```java
@Service
public class DocumentVectorizationService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;
    
    public int processAndVectorizeDocument(MultipartFile file, String kbId, String docId) {
        // 1. 解析文档
        Document document = parser.parse(file.getInputStream());
        
        // 2. 分割文档
        List<TextSegment> segments = splitter.split(document);
        
        // 3. 向量化并存储
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
        
        return segments.size();
    }
    
    public List<EmbeddingMatch<TextSegment>> searchRelevant(String query, int maxResults, String kbId) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        return embeddingStore.search(queryEmbedding, maxResults);
    }
}
```

**功能说明：**
- 文档解析（支持 PDF、Word、Excel、TXT 等）
- 文本分块（递归分块策略）
- 向量化（使用通义千问嵌入模型）
- 向量检索（相似度搜索）

### 3. RagService - RAG 核心服务

```java
@Service
public class RagService {
    
    @Autowired
    private LanguageModel languageModel;
    
    @Autowired
    private DocumentVectorizationService vectorizationService;
    
    public ChatResponse chat(ChatRequest request) {
        // 1. 检索相关文档
        List<EmbeddingMatch<TextSegment>> matches = 
            vectorizationService.searchRelevant(query, 5, kbId);
        
        // 2. 构建上下文
        String context = buildContext(matches);
        
        // 3. 构建提示词
        String prompt = buildPrompt(query, context);
        
        // 4. 生成回答
        String answer = languageModel.generate(prompt);
        
        return ChatResponse.success(answer);
    }
}
```

**RAG 流程：**
```
用户提问 → 向量化 → 检索相关文档 → 构建提示词 → LLM 生成 → 返回答案
```

---

## 📝 使用示例

### 1. 上传文档到知识库

```java
@PostMapping("/upload")
public DocumentInfo uploadDocument(@RequestParam("file") MultipartFile file,
                                   @RequestParam("kbId") String kbId) {
    String docId = UUID.randomUUID().toString();
    
    // 1. 保存文件
    DocumentInfo docInfo = documentService.saveDocument(file, kbId, docId);
    
    // 2. 向量化处理
    int segments = vectorizationService.processAndVectorizeDocument(file, kbId, docId);
    
    docInfo.setSegmentCount(segments);
    return docInfo;
}
```

### 2. 智能问答

```java
@PostMapping("/ask")
public ChatResponse ask(@RequestBody ChatRequest request) {
    return ragService.chat(request);
}
```

**请求示例：**
```json
{
  "query": "什么是电力中长期交易？",
  "kbId": "KB001"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "answer": "电力中长期交易是指发电企业、售电企业、电力用户等市场主体通过双边协商、集中竞价等方式，提前数月或数年开展的电力交易...",
  "sessionId": "session_123456",
  "references": ["DOC001", "DOC002"]
}
```

---

## 🎨 RAG 工作流程

### 文档处理流程
```
上传文档 → Apache Tika 解析 → 文本分块 → 向量化 → 存储到向量数据库
```

### 问答流程
```
用户提问 → 问题向量化 → 检索 Top-K 相关文档 → 构建提示词 → LLM 生成答案 → 返回
```

### 提示词构建
```java
private String buildPrompt(String query, String context) {
    return "您是一位电力交易领域的专业助手，名叫\"小电\"。请用友好、专业的语气回答问题。\n\n" +
           context +
           "用户问题：" + query + "\n\n" +
           "请根据上述参考资料，用通俗易懂的语言回答用户的问题。";
}
```

---

## 🚀 部署和运行

### 1. 环境准备

```bash
# Java 8+
java -version

# Maven 3.6+
mvn -version

# MySQL 8.0+
mysql --version

# Redis (可选)
redis-server --version
```

### 2. 配置 API Key

```bash
# Linux/Mac
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx

# Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-xxxxxxxxxxxxxxxxxxxxxxxx"
```

### 3. 编译项目

```bash
cd d:\project2\power-trade-rag
mvn clean install -DskipTests
```

### 4. 启动服务

```bash
# 启动后端
cd power-trade-rag-api
mvn spring-boot:run

# 启动前端
cd power-trade-rag-web
npm run dev
```

### 5. 访问系统

- 前端：http://localhost:3001/
- 后端 API：http://localhost:8080/
- API 文档：http://localhost:8080/swagger-ui.html

---

## 📊 性能优化建议

### 1. 向量数据库选择

**当前：内存存储**
- ✅ 简单、快速
- ❌ 不支持持久化、不支持删除

**推荐升级：**
- **ChromaDB** - 轻量级、易部署
- **Milvus** - 企业级、高性能
- **Elasticsearch** - 全文检索 + 向量检索

### 2. 缓存策略

```java
@Service
public class RagService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public ChatResponse chat(ChatRequest request) {
        // 1. 检查缓存
        String cachedAnswer = getCachedAnswer(request.getQuery());
        if (cachedAnswer != null) {
            return ChatResponse.success(cachedAnswer);
        }
        
        // 2. 查询 LLM
        ChatResponse response = queryLLM(request);
        
        // 3. 缓存答案
        cacheAnswer(request.getQuery(), response.getAnswer());
        
        return response;
    }
}
```

### 3. 批量向量化

```java
// 批量处理多个文档
public void batchVectorize(List<Document> documents) {
    List<Embedding> embeddings = embeddingModel.embedAll(documents).content();
    for (int i = 0; i < documents.size(); i++) {
        embeddingStore.add(embeddings.get(i), documents.get(i).text());
    }
}
```

---

## 🔍 调试和监控

### 1. 启用日志

```yaml
logging:
  level:
    dev.langchain4j: DEBUG
    com.powertrade.core: DEBUG
```

### 2. 监控指标

```java
@Component
public class RagMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void recordQuery(String query, long duration) {
        meterRegistry.timer("rag.query.duration")
            .record(duration, TimeUnit.MILLISECONDS);
        
        meterRegistry.counter("rag.query.count").increment();
    }
}
```

---

## ⚠️ 常见问题

### 1. API Key 无效

**错误信息：**
```
Invalid API Key provided
```

**解决方案：**
- 检查 API Key 是否正确
- 确认阿里云账号已开通 DashScope 服务
- 检查 API Key 权限

### 2. 依赖下载失败

**错误信息：**
```
Could not resolve dependencies for project
```

**解决方案：**
- 检查 Maven 镜像配置
- 使用阿里云镜像
- 清理本地仓库：`rm -rf ~/.m2/repository/dev/langchain4j`

### 3. 内存溢出

**错误信息：**
```
java.lang.OutOfMemoryError: Java heap space
```

**解决方案：**
- 增加 JVM 堆内存：`-Xmx2g`
- 减小分块大小
- 使用外部向量数据库

---

## 📚 参考资料

### 官方文档
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [阿里云 DashScope 文档](https://help.aliyun.com/zh/dashscope/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)

### 代码示例
- [LangChain4j Examples](https://github.com/langchain4j/langchain4j-examples)
- [DashScope Examples](https://github.com/aliyun/alibabacloud-dashscope-java-demo)

---

## 🎯 下一步

### 功能扩展
1. ✅ 支持多轮对话
2. ✅ 添加引用来源
3. ✅ 支持多种文档格式
4. ⏳ 实现知识库管理界面
5. ⏳ 添加用户权限管理

### 性能优化
1. ⏳ 迁移到 ChromaDB
2. ⏳ 实现答案缓存
3. ⏳ 添加异步处理
4. ⏳ 实现监控告警

---

## ✨ 总结

本集成方案提供了：
- ✅ **完整的 RAG 流程**：文档解析 → 向量化 → 检索 → 生成
- ✅ **Spring Boot 集成**：自动配置、依赖注入
- ✅ **阿里云通义千问**：中文优化、性价比高
- ✅ **灵活的架构**：可替换组件、易于扩展
- ✅ **详细文档**：配置说明、使用示例、故障排查

**立即开始使用 LangChain4j 构建您的智能问答系统！** 🚀

---

**文档版本**：1.0.0  
**更新时间**：2026-05-04  
**适用版本**：LangChain4j 0.31.0 + Spring Boot 2.7.14
