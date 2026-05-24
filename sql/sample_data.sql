-- 示例数据脚本
USE power_trade_rag;

SET NAMES utf8mb4;

INSERT INTO knowledge_base (kb_id, name, description, status, creator) VALUES
('KB001', '电力交易政策法规', '包含国家及地方电力交易相关政策法规', 1, 'admin'),
('KB002', '电力市场交易案例', '历年电力市场交易典型案例', 1, 'admin'),
('KB003', '电力价格机制', '电价形成机制及价格政策', 1, 'admin'),
('KB004', '新能源交易', '新能源发电交易规则', 1, 'admin');

INSERT INTO document_info (doc_id, title, file_name, file_path, file_size, doc_type, content, status, creator) VALUES
('DOC001', '电力中长期交易规则', 'power_trade_rules.pdf', '/data/docs/power_trade_rules.pdf', 1024000, 'POLICY', '电力中长期交易基本规则，包括交易组织、交易方式、结算方式等内容', 1, 'admin'),
('DOC002', '现货市场交易规则', 'spot_market_rules.pdf', '/data/docs/spot_market_rules.pdf', 856000, 'POLICY', '电力现货市场交易基本规则，包括日前市场、实时市场等内容', 1, 'admin'),
('DOC003', '可再生能源电力消纳保障机制', 'renewable_energy.pdf', '/data/docs/renewable_energy.pdf', 512000, 'POLICY', '关于建立健全可再生能源电力消纳保障机制的通知', 1, 'admin'),
('DOC004', '跨省跨区电力交易规则', 'cross_region_trade.pdf', '/data/docs/cross_region_trade.pdf', 768000, 'POLICY', '跨省跨区电力交易相关规定', 1, 'admin');

INSERT INTO document_knowledge_base (doc_id, kb_id, creator) VALUES
('DOC001', 'KB001', 'admin'),
('DOC002', 'KB001', 'admin'),
('DOC003', 'KB001', 'admin'),
('DOC004', 'KB001', 'admin'),
('DOC001', 'KB002', 'admin'),
('DOC002', 'KB002', 'admin');

INSERT INTO chat_record (session_id, query, response, doc_references, creator) VALUES
('session_001', '什么是电力中长期交易', '电力中长期交易是指发电企业、售电企业、电力用户等市场主体通过双边协商、集中竞价等方式，提前数月或数年开展的电力交易。主要包括年度、月度、周交易品种，以及合同转让交易等。', 'DOC001,DOC002', 'admin'),
('session_001', '电力现货市场包含哪些部分', '电力现货市场主要包括日前市场和实时市场。日前市场是提前一天进行的电力交易，用于确定次日的发电计划和用电计划；实时市场是运行日当天根据实际情况进行的调整交易，用于平衡供需偏差。', 'DOC001,DOC002', 'admin');
