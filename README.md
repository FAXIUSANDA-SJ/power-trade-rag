# 电力交易智能问答系统（Power Trade RAG）

基于RAG（检索增强生成）技术的企业级智能问答系统，专为电力交易行业设计。系统整合了电力政策文档、交易数据和历史案例，通过自然语言查询提供智能化的答案。

## 技术架构

- **后端**: Spring Boot 2.7.x + MyBatis Plus + Redis
- **RAG引擎**: LangChain4j + Qwen API
- **前端**: Vue3 + Element Plus
- **数据库**: MySQL 8.0
- **向量数据库**: ChromaDB
- **文档解析**: Apache Tika
- **API文档**: Swagger

## 项目目录结构

```
power-trade-rag/
├── power-trade-rag-api/           # API网关层
│   ├── src/main/java/com/powertrade/api/
│   │   ├── controller/            # 控制器
│   │   └── ApiApplication.java    # 启动类
│   └── pom.xml
├── power-trade-rag-core/          # 核心业务层
│   ├── src/main/java/com/powertrade/core/
│   │   ├── service/               # 服务层
│   │   ├── model/                 # 模型层
│   │   └── config/                # 配置层
│   └── pom.xml
├── power-trade-rag-dal/           # 数据访问层
│   ├── src/main/java/com/powertrade/dal/
│   │   ├── mapper/                # 数据映射
│   │   └── entity/                # 实体类
│   └── pom.xml
├── power-trade-rag-common/        # 公共模块
│   ├── src/main/java/com/powertrade/common/
│   │   ├── constant/              # 常量定义
│   │   ├── exception/             # 异常处理
│   │   └── util/                  # 工具类
│   └── pom.xml
├── power-trade-rag-web/           # 前端模块
│   ├── src/
│   │   ├── components/            # 组件
│   │   ├── views/                 # 视图
│   │   └── api/                   # API调用
│   ├── package.json
│   └── vite.config.js
├── sql/                           # 数据库脚本
│   ├── init_db.sql                # 初始化脚本
│   └── sample_data.sql            # 示例数据
├── docker/                        # Docker配置
│   └── docker-compose.yml
├── docs/                          # 文档
└── pom.xml                        # Maven父工程
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0
- Docker & Docker Compose

### 使用Docker部署

```bash
cd docker
docker-compose up -d
```

### 手动部署

#### 1. 数据库初始化

```bash
mysql -u root -p < sql/init_db.sql
mysql -u root -p power_trade_rag < sql/sample_data.sql
```

#### 2. 启动后端

```bash
mvn clean install
cd power-trade-rag-api
mvn spring-boot:run
```

#### 3. 启动前端

```bash
cd power-trade-rag-web
npm install
npm run dev
```

## 访问地址

- **后端API**: http://localhost:8080
- **Swagger文档**: http://localhost:8080/swagger-ui.html
- **前端界面**: http://localhost:3000

## 使用流程

1. 上传电力政策文档到系统
2. 系统自动解析并构建向量索引
3. 通过聊天界面提问，获取基于文档的智能回答

## 核心API

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 智能问答 | POST | /api/chat/ask | 发送问题获取答案 |
| 上传文档 | POST | /api/document/upload | 上传文档到知识库 |
| 获取文档列表 | GET | /api/document/list | 获取文档列表 |
| 删除文档 | DELETE | /api/document/{docId} | 删除指定文档 |
| 创建知识库 | POST | /api/knowledge/create | 创建新知识库 |
| 获取知识库列表 | GET | /api/knowledge/list | 获取知识库列表 |
| 删除知识库 | DELETE | /api/knowledge/{kbId} | 删除指定知识库 |

## 项目特点

- **企业级架构**: 分层设计，模块化开发，便于维护和扩展
- **高性能**: 使用Redis缓存热点数据，ChromaDB向量检索
- **易扩展**: 支持多种文档格式，可轻松接入不同AI模型
- **安全性**: 包含访问控制和敏感信息过滤机制
- **监控运维**: 集成健康检查和性能监控

## 许可证

MIT License