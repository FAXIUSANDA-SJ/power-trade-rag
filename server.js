const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');

const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

let pool;

async function initDB() {
    pool = mysql.createPool({
        host: 'localhost',
        user: 'root',
        password: 'root',
        database: 'power_trade_rag',
        waitForConnections: true,
        connectionLimit: 10
    });
    console.log('MySQL连接池已创建');
}

initDB();

// ========== 系统提示配置 ===========
const SYSTEM_PROMPT = {
    // 智能体人设
    persona: '您是一位友好、专业的电力交易智能助手，名叫"小电"。您说话亲切、有耐心，善于用通俗易懂的语言解释专业概念。',
    
    // 问候语
    greeting: '您好！我是您的电力交易智能助手小电⚡，很高兴为您服务！有任何电力交易相关的问题都可以问我哦~',
    
    // 回答风格
    style: {
        friendly: true,           // 友好亲切
        professional: true,        // 专业准确
        useEmoji: true,           // 适度使用表情符号
        structured: true,         // 结构化回答
        examples: true            // 提供实例
    },
    
    // 常用问候和结束语
    phrases: {
        start: ['您好呀！', '很高兴为您解答！', '没问题，让我来帮您看看~', '好的，这个问题我来解答！'],
        end: ['希望能帮到您！', '如果还有疑问，随时问我哦~', '祝您工作顺利！', '有其他问题欢迎继续提问！'],
        uncertain: ['这个问题我需要查证一下~', '让我想想怎么解释更清楚~', '我尽量用简单的方式说明~']
    }
};

const qaDatabase = {
    '什么是电力中长期交易': '电力中长期交易是指发电企业、售电企业、电力用户等市场主体通过双边协商、集中竞价等方式，提前数月或数年开展的电力交易。主要包括年度、月度、周交易品种，以及合同转让交易等。',
    '电力现货市场包含哪些部分': '电力现货市场主要包括日前市场和实时市场。日前市场是提前一天进行的电力交易，用于确定次日的发电计划和用电计划；实时市场是运行日当天根据实际情况进行的调整交易，用于平衡供需偏差。',
    '什么是可再生能源电力消纳': '可再生能源电力消纳是指通过各种措施，确保风电、光伏等可再生能源发的电能够被电网全部接纳和使用。国家建立了可再生能源电力消纳保障机制，通过权重指标考核等方式推动消纳工作。',
    '电力交易价格如何确定': '电力交易价格通过多种方式确定：1）双边协商交易价格由买卖双方自主协商；2）集中竞价交易按价格排序匹配形成；3）挂牌交易按挂牌价格成交。市场价格受供需关系、燃料成本、电网约束等因素影响。',
    '什么是电力辅助服务': '电力辅助服务是指为维护电力系统安全稳定运行所提供的服务，包括调频、调压、备用、黑启动等。辅助服务费用由发电企业和电力用户共同承担。'
};

// 人性化回答生成器
function generateHumanizedAnswer(query, baseAnswer) {
    const greetings = SYSTEM_PROMPT.phrases.start;
    const endings = SYSTEM_PROMPT.phrases.end;
    
    // 随机选择开场白
    const greeting = greetings[Math.floor(Math.random() * greetings.length)];
    
    // 随机选择结束语
    const ending = endings[Math.floor(Math.random() * endings.length)];
    
    // 根据问题类型添加适当的表情符号
    const emojis = {
        '什么': '🤔',
        '如何': '💡',
        '怎么': '🔍',
        '哪些': '📋',
        '为什么': '❓',
        '交易': '⚡',
        '电力': '💡',
        '市场': '📊',
        '价格': '💰'
    };
    
    let emoji = '';
    for (const [keyword, e] of Object.entries(emojis)) {
        if (query.includes(keyword)) {
            emoji = e + ' ';
            break;
        }
    }
    
    // 结构化回答
    let answer = baseAnswer;
    
    // 如果答案较长，尝试分段
    if (answer.length > 100) {
        const sentences = answer.split(/[.。;；]/).filter(s => s.trim().length > 0);
        if (sentences.length > 1) {
            answer = sentences.map((s, i) => `${i + 1}. ${s}`).join('\n\n');
        }
    }
    
    // 组合完整的回答
    return `${emoji}${greeting}\n\n${answer}\n\n${ending}😊`;
}

function findBestAnswer(query) {
    const lowerQuery = query.toLowerCase().trim();
    
    // 处理问候语
    if (['你好', '您好', 'hello', 'hi', '早上好', '下午好', '晚上好'].some(g => lowerQuery.includes(g))) {
        return SYSTEM_PROMPT.greeting;
    }
    
    // 处理感谢
    if (['谢谢', '感谢', 'thanks', 'thank you'].some(g => lowerQuery.includes(g))) {
        return '不客气哦！能帮到您我很开心~ 有其他问题随时问我！😊';
    }
    
    // 处理告别
    if (['再见', '拜拜', 'bye', 'goodbye'].some(g => lowerQuery.includes(g))) {
        return '再见！祝您工作顺利，有任何问题随时欢迎回来找我！👋';
    }
    
    // 精确匹配问题
    for (const [key, value] of Object.entries(qaDatabase)) {
        if (lowerQuery === key.toLowerCase()) {
            return generateHumanizedAnswer(query, value);
        }
    }
    
    // 模糊匹配问题
    for (const [key, value] of Object.entries(qaDatabase)) {
        if (lowerQuery.includes(key.toLowerCase()) || key.toLowerCase().includes(lowerQuery)) {
            return generateHumanizedAnswer(query, value);
        }
    }
    
    // 未找到匹配时的友好回复
    const uncertainPhrases = SYSTEM_PROMPT.phrases.uncertain;
    const uncertainPhrase = uncertainPhrases[Math.floor(Math.random() * uncertainPhrases.length)];
    
    return `${uncertainPhrase}\n\n您问的"${query}"是一个很有深度的问题。由于我的知识库还在不断学习中，建议您：\n\n1. 查阅相关的电力交易政策法规\n2. 咨询专业的电力交易机构\n3. 参考电力交易中心发布的官方文件\n\n如果您有其他具体问题，我很乐意为您解答！📚`;
}

app.post('/api/chat/ask', async (req, res) => {
    const { sessionId, query, kbId } = req.body;
    
    const sid = sessionId || 'session_' + Date.now();
    
    const answer = findBestAnswer(query);
    
    try {
        if (pool) {
            await pool.execute(
                'INSERT INTO chat_record (session_id, query, response, doc_references, creator) VALUES (?, ?, ?, ?, ?)',
                [sid, query, answer, 'DOC001,DOC002', 'user']
            );
        }
    } catch (err) {
        console.error('保存聊天记录失败:', err);
    }
    
    res.json({
        answer: answer,
        sessionId: sid,
        references: ['DOC001', 'DOC002'],
        code: 200,
        message: 'success'
    });
});

app.get('/api/knowledge/list', async (req, res) => {
    try {
        if (!pool) {
            return res.json([]);
        }
        const [rows] = await pool.execute('SELECT * FROM knowledge_base WHERE status = 1');
        res.json(rows);
    } catch (err) {
        console.error('获取知识库列表失败:', err);
        res.json([]);
    }
});

app.post('/api/knowledge/create', async (req, res) => {
    try {
        if (!pool) {
            return res.status(500).json({ message: '数据库未连接' });
        }
        const kbId = 'KB' + Date.now();
        const { name, description, status } = req.body;
        await pool.execute(
            'INSERT INTO knowledge_base (kb_id, name, description, status, creator) VALUES (?, ?, ?, ?, ?)',
            [kbId, name, description, status || 1, 'admin']
        );
        res.json({ kbId, name, description, status: status || 1, creator: 'admin' });
    } catch (err) {
        console.error('创建知识库失败:', err);
        res.status(500).json({ message: '创建失败' });
    }
});

app.put('/api/knowledge/:kbId', async (req, res) => {
    try {
        if (!pool) {
            return res.status(500).json({ message: '数据库未连接' });
        }
        const { name, description, status } = req.body;
        await pool.execute(
            'UPDATE knowledge_base SET name = ?, description = ?, status = ? WHERE kb_id = ?',
            [name, description, status, req.params.kbId]
        );
        res.json({ success: true, kbId: req.params.kbId, name, description, status });
    } catch (err) {
        console.error('更新知识库失败:', err);
        res.status(500).json({ message: '更新失败' });
    }
});

app.delete('/api/knowledge/:kbId', async (req, res) => {
    try {
        if (!pool) {
            return res.status(500).json({ message: '数据库未连接' });
        }
        await pool.execute('DELETE FROM knowledge_base WHERE kb_id = ?', [req.params.kbId]);
        res.json({ success: true });
    } catch (err) {
        console.error('删除知识库失败:', err);
        res.status(500).json({ success: false, message: '删除失败' });
    }
});

app.get('/api/chat/stats', async (req, res) => {
    try {
        if (!pool) {
            return res.json({ total: 0 });
        }
        const [rows] = await pool.execute('SELECT COUNT(*) as total FROM chat_record');
        res.json({ total: rows[0]?.total || 0 });
    } catch (err) {
        console.error('获取统计数据失败:', err);
        res.json({ total: 0 });
    }
});

app.get('/api/document/list', async (req, res) => {
    try {
        if (!pool) {
            return res.json([]);
        }
        const [rows] = await pool.execute('SELECT * FROM document_info WHERE status = 1');
        res.json(rows);
    } catch (err) {
        console.error('获取文档列表失败:', err);
        res.json([]);
    }
});

app.post('/api/document/upload', async (req, res) => {
    try {
        if (!pool) {
            return res.status(500).json({ message: '数据库未连接' });
        }
        const docId = 'DOC' + Date.now();
        const { title, fileName } = req.body;
        await pool.execute(
            'INSERT INTO document_info (doc_id, title, file_name, file_path, file_size, doc_type, content, status, creator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)',
            [docId, title, fileName || title + '.pdf', '/data/docs/' + (fileName || title + '.pdf'), 0, 'PDF', '', 1, 'admin']
        );
        res.json({ docId, title, fileName, status: 1 });
    } catch (err) {
        console.error('上传文档失败:', err);
        res.status(500).json({ message: '上传失败' });
    }
});

app.delete('/api/document/:docId', async (req, res) => {
    try {
        if (!pool) {
            return res.status(500).json({ message: '数据库未连接' });
        }
        await pool.execute('DELETE FROM document_info WHERE doc_id = ?', [req.params.docId]);
        res.json({ success: true });
    } catch (err) {
        console.error('删除文档失败:', err);
        res.status(500).json({ success: false, message: '删除失败' });
    }
});

app.listen(PORT, () => {
    console.log(`后端服务已启动: http://localhost:${PORT}`);
});