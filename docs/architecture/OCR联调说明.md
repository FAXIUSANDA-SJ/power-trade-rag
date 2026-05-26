# OCR 联调说明

## 1. 目标

- 验证当前 OCR provider 配置是否生效。
- 验证鉴权头、请求字段、响应字段映射是否正确。
- 验证 OCR 调试面板是否能展示结构化结果。

## 2. 相关入口

- 后端接口：`POST /api/document/ocr/test`
- 前端页面：`文档管理 -> OCR 调试面板`
- 后端实现：
  - `power-trade-rag-api/src/main/java/com/powertrade/api/controller/DocumentController.java`
  - `power-trade-rag-core/src/main/java/com/powertrade/core/service/rag/ConfigurableOcrService.java`
  - `power-trade-rag-core/src/main/java/com/powertrade/core/service/rag/HttpOcrProvider.java`

## 3. 关键配置项

在 `application.yml` 和 `.env.example` 中已预留以下配置：

- `RAG_OCR_PROVIDER`
- `RAG_OCR_ENDPOINT`
- `RAG_OCR_API_KEY`
- `RAG_OCR_AUTH_HEADER`
- `RAG_OCR_AUTH_PREFIX`
- `RAG_OCR_REQUEST_FILE_FIELD`
- `RAG_OCR_RESPONSE_FIELD`
- `RAG_OCR_RESPONSE_SUCCESS_FIELD`
- `RAG_OCR_RESPONSE_MESSAGE_FIELD`
- `RAG_OCR_RESPONSE_CODE_FIELD`
- `RAG_OCR_SUCCESS_VALUES`
- `RAG_OCR_MAX_FILE_SIZE_BYTES`

## 4. 推荐配置示例

### 场景 A：Bearer 鉴权 + 顶层 text 字段

```env
RAG_OCR_PROVIDER=http
RAG_OCR_ENDPOINT=https://your-ocr.example.com/api/ocr
RAG_OCR_API_KEY=your-api-key
RAG_OCR_AUTH_HEADER=Authorization
RAG_OCR_AUTH_PREFIX=Bearer 
RAG_OCR_REQUEST_FILE_FIELD=fileBase64
RAG_OCR_RESPONSE_FIELD=text
RAG_OCR_RESPONSE_SUCCESS_FIELD=success
RAG_OCR_RESPONSE_MESSAGE_FIELD=message
RAG_OCR_RESPONSE_CODE_FIELD=code
RAG_OCR_SUCCESS_VALUES=true,0,200,ok,success
RAG_OCR_MAX_FILE_SIZE_BYTES=5242880
```

### 场景 B：自定义鉴权头 + 嵌套 data.text 字段

```env
RAG_OCR_PROVIDER=http
RAG_OCR_ENDPOINT=https://your-ocr.example.com/recognize
RAG_OCR_API_KEY=your-api-key
RAG_OCR_AUTH_HEADER=X-API-Key
RAG_OCR_AUTH_PREFIX=
RAG_OCR_REQUEST_FILE_FIELD=file
RAG_OCR_RESPONSE_FIELD=data.text
RAG_OCR_RESPONSE_SUCCESS_FIELD=data.success
RAG_OCR_RESPONSE_MESSAGE_FIELD=data.message
RAG_OCR_RESPONSE_CODE_FIELD=data.code
RAG_OCR_SUCCESS_VALUES=true,0,200,ok,success
```

## 5. 联调步骤

1. 配置 `.env` 或部署环境中的 OCR 变量。
2. 启动后端和前端。
3. 打开 `文档管理` 页面。
4. 在 `OCR 调试面板` 选择测试文件，优先使用：
   - 1 个图片扫描件
   - 1 个扫描 PDF
   - 1 个普通文本型 PDF
5. 点击 `开始 OCR 测试`。
6. 检查页面返回：
   - `provider`
   - `success`
   - `errorType`
   - `errorCode`
   - `httpStatus`
   - `responsePreview`
   - `extractedText`

## 6. 常见错误与定位

- `OCR_PROVIDER_NOT_CONFIGURED`
  - 含义：未配置 `RAG_OCR_ENDPOINT`
  - 处理：检查 endpoint 是否注入成功
- `OCR_PROVIDER_DISABLED`
  - 含义：当前 provider 为 `none`
  - 处理：将 `RAG_OCR_PROVIDER` 设置为 `http`
- `OCR_FILE_TOO_LARGE`
  - 含义：测试文件超过最大限制
  - 处理：调大 `RAG_OCR_MAX_FILE_SIZE_BYTES` 或换小文件
- `OCR_RESPONSE_INVALID`
  - 含义：响应字段路径不匹配，或 OCR 返回空文本
  - 处理：检查 `RAG_OCR_RESPONSE_*` 配置
- `OCR_REQUEST_FAILED`
  - 含义：远端服务异常、网络问题或返回状态异常
  - 处理：检查网络连通性、鉴权头和服务端日志

## 7. 验收标准

- 能成功识别至少 1 个扫描件或扫描 PDF。
- 页面能看到结构化返回字段，而不只是纯文本。
- 典型异常能落到明确的 `errorType` 和 `errorCode`。
- 错误信息与响应摘要可用于排查第三方 OCR 配置问题。
