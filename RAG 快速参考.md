# RAG 系统快速参考指南

## 🚀 核心服务一览

### 服务架构

```
RagCoreService (核心编排)
    ├── DocumentPreprocessingService (文档预处理)
    ├── VectorStoreService (向量存储)
    ├── SimilaritySearchEngine (相似度检索)
    └── AnswerGenerationService (答案生成)
```

---

## 📦 核心类和方法

### 1. RagCoreService - 使用示例

```java
@Autowired
private RagCoreService ragCoreService;

// 智能问答
ChatResponse response = ragCoreService.chat(request);

// 上传文档
int segments = ragCoreService.processAndStoreDocument(file, kbId, docId);

// 上传文本
int segments = ragCoreService.processAndStoreText(text, kbId, docId);

// 获取统计
KnowledgeBaseStats stats = ragCoreService.getKnowledgeBaseStats(kbId);

// 测试检索
SearchResult result = ragCoreService.testSearch(query, kbId);
```

### 2. DocumentPreprocessingService

```java
@Autowired
private DocumentPreprocessingService preprocessingService;

// 解析文档
Document doc = preprocessingService.parseDocument(file);

// 清洗文本
String cleaned = preprocessingService.cleanText(text);

// 分割文档
List<TextSegment> segments = preprocessingService.splitDocument(doc);

// 完整预处理
List<TextSegment> segments = preprocessingService.preprocessDocument(file, docId, kbId);
```

### 3. VectorStoreService

```java
@Autowired
private VectorStoreService vectorStoreService;

// 向量化单个片段
String vectorId = vectorStoreService.addTextSegment(segment);

// 批量向量化
List<String> ids = vectorStoreService.addTextSegments(segments);

// 检索
List<EmbeddingMatch<TextSegment>> matches = 
    vectorStoreService.searchSimilar(query, 5, kbId);

// 统计
KnowledgeBaseStats stats = vectorStoreService.getKnowledgeBaseStats(kbId);
```

### 4. SimilaritySearchEngine

```java
@Autowired
private SimilaritySearchEngine searchEngine;

// 构建检索请求
SearchRequest request = searchEngine.builder()
    .query("电力交易")
    .maxResults(5)
    .kbId("KB001")
    .minScore(0.6);

// 执行检索
SearchResult result = searchEngine.search(request);

// 简单检索
List<MatchedDocument> docs = searchEngine.search(query, 5, kbId);
```

### 5. AnswerGenerationService

```java
@Autowired
private AnswerGenerationService answerService;

// 生成答案
String answer = answerService.generateAnswer(context, query);

// 生成带引用的答案
AnswerResult result = answerService.generateAnswerWithReferences(docs, query);

// 自定义提示词
String answer = answerService.generateWithCustomPrompt(prompt);

// 使用提示词构建器
String prompt = answerService.promptBuilder()
    .systemPrompt("您是专业助手...")
    .context(context)
    .query(query)
    .build();
```

---

## 🔧 API 接口

### 智能问答

```bash
# 提问
POST /api/chat/ask
Content-Type: application/json

{
  "query": "什么是电力中长期交易？",
  "kbId": "KB001",
  "sessionId": "session_123"  // 可选
}

# 测试检索
POST /api/chat/search?query=电力交易&kbId=KB001
```

### 文档管理

```bash
# 上传文档
POST /api/document/upload
Content-Type: multipart/form-data

file: (文件)
kbId: KB001

# 上传文本
POST /api/document/uploadText
Content-Type: application/json

{
  "text": "电力中长期交易是指...",
  "kbId": "KB001"
}

# 获取列表
GET /api/document/list?kbId=KB001&page=1&size=10

# 删除文档
DELETE /api/document/{docId}

# 获取统计
GET /api/document/stats/{kbId}
```

---

## 📊 数据结构

### ChatRequest

```json
{
  "query": "string",           // 用户问题（必填）
  "kbId": "string",            // 知识库 ID（可选）
  "sessionId": "string"        // 会话 ID（可选）
}
```

### ChatResponse

```json
{
  "code": 200,                 // 状态码
  "message": "success",        // 消息
  "answer": "string",          // 答案内容
  "sessionId": "string",       // 会话 ID
  "references": ["DOC1", "DOC2"]  // 引用文档 ID
}
```

### DocumentInfo

```json
{
  "docId": "DOC123",
  "fileName": "文件.pdf",
  "kbId": "KB001",
  "segmentCount": 10,
  "uploadTime": "2026-05-04T10:00:00"
}
```

### KnowledgeBaseStats

```json
{
  "knowledgeBaseId": "KB001",
  "vectorCount": 150,
  "documentCount": 10
}
```

### SearchResult

```json
{
  "query": "电力交易",
  "totalMatches": 5,
  "searchTimeMs": 120,
  "documents": [
    {
      "text": "内容...",
      "score": 0.89,
      "docId": "DOC123",
      "fileName": "文件.pdf",
      "segmentIndex": 2,
      "metadata": {}
    }
  ]
}
```

---

## ⚙️ 配置参数

### application.yml

```yaml
# 文档分块
rag:
  document:
    chunk-size: 300            # 分块大小
    chunk-overlap: 30          # 重叠大小
  
  # LLM 参数
  llm:
    temperature: 0.7           # 创造性
    max-tokens: 2000           # 最大长度
  
  # 检索参数
  search:
    default-max-results: 5     # 默认结果数
    min-similarity-score: 0.6  # 最低相似度
```

### 参数调优建议

| 场景 | chunk-size | min-score | temperature |
|------|------------|-----------|-------------|
| 技术文档 | 300 | 0.6 | 0.5 |
| 政策法规 | 400 | 0.7 | 0.3 |
| 合同协议 | 500 | 0.65 | 0.3 |
| 通用问答 | 300 | 0.6 | 0.7 |

---

## 🎯 典型使用场景

### 场景 1：上传电力政策文档

```java
@PostMapping("/upload")
public Response upload(@RequestParam("file") MultipartFile file) {
    String kbId = "policy_kb";
    String docId = "DOC" + System.currentTimeMillis();
    
    // 处理并存储
    int segments = ragCoreService.processAndStoreDocument(file, kbId, docId);
    
    return Response.success("处理了 " + segments + " 个片段");
}
```

### 场景 2：智能问答

```java
@PostMapping("/chat")
public ChatResponse chat(@RequestBody ChatRequest request) {
    // 直接使用 RAG 服务
    return ragCoreService.chat(request);
}
```

### 场景 3：批量导入文档

```java
public void batchImport(List<MultipartFile> files, String kbId) {
    for (MultipartFile file : files) {
        String docId = "DOC" + System.currentTimeMillis();
        ragCoreService.processAndStoreDocument(file, kbId, docId);
    }
}
```

### 场景 4：知识库统计

```java
public Map<String, Object> getStats(String kbId) {
    KnowledgeBaseStats stats = ragCoreService.getKnowledgeBaseStats(kbId);
    
    Map<String, Object> result = new HashMap<>();
    result.put("vectors", stats.getVectorCount());
    result.put("documents", stats.getDocumentCount());
    
    return result;
}
```

---

## 🐛 常见问题

### Q1: 文档上传后找不到？

**原因：** 向量存储是内存的，重启后丢失

**解决：**
```java
// 启动时重新加载文档
@PostConstruct
public void init() {
    List<Document> docs = documentService.findAll();
    for (Document doc : docs) {
        // 重新向量化
        ragCoreService.processAndStoreDocument(...);
    }
}
```

### Q2: 检索结果不准确？

**调整参数：**
```java
// 降低最低相似度
SearchRequest request = searchEngine.builder()
    .minScore(0.5)  // 从 0.6 降到 0.5
    .maxResults(10) // 增加结果数
    .build();
```

### Q3: 答案生成太慢？

**优化方案：**
```java
// 1. 使用缓存
String cached = cache.get(query);
if (cached != null) return cached;

// 2. 减少检索结果数
SearchRequest request = searchEngine.builder()
    .maxResults(3)  // 从 5 降到 3
    .build();

// 3. 异步生成
CompletableFuture<String> answer = 
    CompletableFuture.supplyAsync(() -> 
        answerService.generateAnswer(context, query)
    );
```

---

## 📈 性能优化

### 1. 批量处理

```java
// 批量向量化（推荐）
List<String> ids = vectorStoreService.addTextSegments(segments);

// 而不是循环单个添加
for (TextSegment segment : segments) {
    vectorStoreService.addTextSegment(segment);
}
```

### 2. 结果缓存

```java
@Cacheable(value = "answers", key = "#request.query")
public ChatResponse chat(ChatRequest request) {
    return ragCoreService.chat(request);
}
```

### 3. 异步处理

```java
@Async
public CompletableFuture<Integer> processDocument(
    MultipartFile file, String kbId, String docId) {
    
    int segments = ragCoreService.processAndStoreDocument(file, kbId, docId);
    return CompletableFuture.completedFuture(segments);
}
```

---

## 🔍 调试技巧

### 1. 启用详细日志

```yaml
logging:
  level:
    com.powertrade.core.service.rag: DEBUG
    dev.langchain4j: DEBUG
```

### 2. 查看检索结果

```java
// 测试检索（不生成答案）
SearchResult result = ragCoreService.testSearch(query, kbId);
System.out.println("找到 " + result.getTotalMatches() + " 个结果");
for (MatchedDocument doc : result.getDocuments()) {
    System.out.println("分数：" + doc.getScore());
    System.out.println("内容：" + doc.getText());
}
```

### 3. 检查向量存储

```java
List<KnowledgeBaseStats> allStats = ragCoreService.getAllKnowledgeBaseStats();
for (KnowledgeBaseStats stats : allStats) {
    System.out.println("知识库：" + stats.getKnowledgeBaseId());
    System.out.println("向量数：" + stats.getVectorCount());
    System.out.println("文档数：" + stats.getDocumentCount());
}
```

---

## 📚 完整文档

详细文档请参考：
- [RAG 完整实现文档.md](./RAG 完整实现文档.md)
- [LangChain4j 集成指南.md](./LangChain4j 集成指南.md)
- [快速开始.md](./快速开始.md)

---

**最后更新**：2026-05-04  
**版本**：1.0.0
