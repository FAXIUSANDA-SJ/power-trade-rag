-- 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS power_trade_rag CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE power_trade_rag;

-- 文档信息表
CREATE TABLE document_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id VARCHAR(64) NOT NULL COMMENT '文档唯一标识',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    doc_type VARCHAR(50) NOT NULL COMMENT '文档类型',
    content TEXT COMMENT '文档内容',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-删除',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_doc_id (doc_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档信息表';

-- 知识库表
CREATE TABLE knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id VARCHAR(64) NOT NULL COMMENT '知识库唯一标识',
    name VARCHAR(255) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '知识库描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 文档与知识库关联表
CREATE TABLE document_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id VARCHAR(64) NOT NULL COMMENT '文档ID',
    kb_id VARCHAR(64) NOT NULL COMMENT '知识库ID',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_doc_kb (doc_id, kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档与知识库关联表';

-- 对话记录表
CREATE TABLE chat_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    query TEXT NOT NULL COMMENT '用户查询',
    response TEXT NOT NULL COMMENT '系统回复',
    doc_references TEXT COMMENT '引用文档',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-删除',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记录表';

-- 统一配置版本表
CREATE TABLE config_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_type VARCHAR(64) NOT NULL COMMENT '配置类型: prompt/retrieval/knowledge_base',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键: system 或 kb_id',
    version_no INT NOT NULL COMMENT '版本号',
    is_active TINYINT DEFAULT 0 COMMENT '是否为当前生效版本',
    base_version_id BIGINT NULL COMMENT '基于哪个版本创建',
    description VARCHAR(255) NULL COMMENT '版本说明',
    config_payload LONGTEXT NOT NULL COMMENT '配置 JSON',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_version (config_type, config_key, version_no),
    INDEX idx_config_active (config_type, config_key, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一配置版本表';

-- 文档摄取任务表
CREATE TABLE ingest_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL COMMENT '任务唯一标识',
    doc_id VARCHAR(64) NOT NULL COMMENT '文档ID',
    kb_id VARCHAR(64) NOT NULL COMMENT '知识库ID',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型: upload/reindex/delete',
    status VARCHAR(32) NOT NULL COMMENT '任务状态: pending/running/success/failed',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT DEFAULT 3 COMMENT '最大重试次数',
    error_message VARCHAR(1000) NULL COMMENT '错误信息',
    creator VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_doc_task (doc_id, kb_id),
    INDEX idx_task_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档摄取任务表';
