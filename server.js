const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

let pool;
const promptConfigPath = path.join(__dirname, 'data', 'prompt-config.json');

function getDefaultPromptConfig() {
    return {
        assistantName: '小电',
        welcomeMessage: '您好，我是小电，很高兴为您服务。您可以咨询电力交易规则、政策解读、业务流程或知识库相关问题。',
        systemPrompt: [
            '你是一名电力交易领域的专业智能客服，名称为"小电"。',
            '你的核心职责是为用户提供专业、可信、易懂的咨询服务。',
            '请优先结合知识库和当前对话上下文作答，语气保持友好、专业、耐心。'
        ].join('\n'),
        fallbackReply: '抱歉，我暂时没有找到与您问题直接相关的信息。您可以换个问法，或补充更多背景后我继续帮您分析。',
        memoryRounds: 6,
        updatedAt: new Date().toISOString()
    };
}

function normalizePromptConfig(config = {}) {
    const defaultConfig = getDefaultPromptConfig();
    const memoryRounds = Number(config.memoryRounds);

    return {
        assistantName: String(config.assistantName || defaultConfig.assistantName).trim() || defaultConfig.assistantName,
        welcomeMessage: String(config.welcomeMessage || defaultConfig.welcomeMessage).trim() || defaultConfig.welcomeMessage,
        systemPrompt: String(config.systemPrompt || defaultConfig.systemPrompt).trim() || defaultConfig.systemPrompt,
        fallbackReply: String(config.fallbackReply || defaultConfig.fallbackReply).trim() || defaultConfig.fallbackReply,
        memoryRounds: Number.isFinite(memoryRounds) ? Math.min(Math.max(memoryRounds, 1), 20) : defaultConfig.memoryRounds,
        updatedAt: config.updatedAt || new Date().toISOString()
    };
}

function ensurePromptConfigDir() {
    const dir = path.dirname(promptConfigPath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
}

function loadPromptConfig() {
    try {
        ensurePromptConfigDir();
        if (!fs.existsSync(promptConfigPath)) {
            const defaultConfig = getDefaultPromptConfig();
            fs.writeFileSync(promptConfigPath, JSON.stringify(defaultConfig, null, 2), 'utf8');
            return defaultConfig;
        }

        const raw = fs.readFileSync(promptConfigPath, 'utf8');
        return normalizePromptConfig(JSON.parse(raw));
    } catch (error) {
        console.error('加载提示词配置失败，使用默认配置:', error);
        return getDefaultPromptConfig();
    }
}

function savePromptConfig(config) {
    const nextConfig = normalizePromptConfig({
        ...config,
        updatedAt: new Date().toISOString()
    });
    ensurePromptConfigDir();
    fs.writeFileSync(promptConfigPath, JSON.stringify(nextConfig, null, 2), 'utf8');
    return nextConfig;
}

let currentPromptConfig = loadPromptConfig();
const sessionMemory = new Map();

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

const promptPhrases = {
    start: ['您好呀！', '很高兴为您解答！', '没问题，让我来帮您看看~', '好的，这个问题我来解答！'],
    end: ['希望能帮到您！', '如果还有疑问，随时问我哦~', '祝您工作顺利！', '有其他问题欢迎继续提问！'],
    uncertain: ['这个问题我需要查证一下~', '让我想想怎么解释更清楚~', '我尽量用简单的方式说明~']
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
    const greetings = promptPhrases.start;
    const endings = promptPhrases.end;
    
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
        return currentPromptConfig.welcomeMessage;
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
    const uncertainPhrases = promptPhrases.uncertain;
    const uncertainPhrase = uncertainPhrases[Math.floor(Math.random() * uncertainPhrases.length)];
    
    return `${uncertainPhrase}\n\n${currentPromptConfig.fallbackReply}\n\n如果您愿意，也可以补充更具体的业务背景、场景或关键词，我会继续帮您分析。📚`;
}

function getRecentHistory(sessionId) {
    const memory = sessionMemory.get(sessionId) || [];
    const size = currentPromptConfig.memoryRounds || 6;
    return memory.slice(-size);
}

function appendSessionMemory(sessionId, query, answer) {
    const history = sessionMemory.get(sessionId) || [];
    history.push({
        query,
        answer,
        time: new Date().toISOString()
    });
    const size = currentPromptConfig.memoryRounds || 6;
    sessionMemory.set(sessionId, history.slice(-size));
}

app.post('/api/chat/ask', async (req, res) => {
    const { sessionId, query, kbId } = req.body;
    
    const sid = sessionId || 'session_' + Date.now();
    const history = getRecentHistory(sid);
    let answer = findBestAnswer(query);

    if (history.length > 0 && ['继续', '刚才', '上一个', '前面', '那个问题'].some(keyword => query.includes(keyword))) {
        const lastTurn = history[history.length - 1];
        answer = `结合我们上一轮的对话，您刚才咨询的是：${lastTurn.query}\n\n${answer}`;
    }
    
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

    appendSessionMemory(sid, query, answer);
    
    res.json({
        answer: answer,
        sessionId: sid,
        references: ['DOC001', 'DOC002'],
        code: 200,
        message: 'success'
    });
});

app.delete('/api/chat/session/:sessionId', (req, res) => {
    sessionMemory.delete(req.params.sessionId);
    res.json({ message: '会话记忆已清空' });
});

app.get('/api/prompt-config', (req, res) => {
    currentPromptConfig = loadPromptConfig();
    res.json(currentPromptConfig);
});

app.put('/api/prompt-config', (req, res) => {
    try {
        currentPromptConfig = savePromptConfig(req.body || {});
        res.json(currentPromptConfig);
    } catch (error) {
        console.error('保存提示词配置失败:', error);
        res.status(500).json({ message: '保存提示词配置失败' });
    }
});

app.post('/api/prompt-config/reset', (req, res) => {
    try {
        currentPromptConfig = savePromptConfig(getDefaultPromptConfig());
        res.json(currentPromptConfig);
    } catch (error) {
        console.error('重置提示词配置失败:', error);
        res.status(500).json({ message: '重置提示词配置失败' });
    }
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
