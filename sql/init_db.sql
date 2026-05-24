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