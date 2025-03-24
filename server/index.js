const express = require('express');
const cors = require('cors');
const axios = require('axios');
const app = express();

// CORS配置
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-File-Format'],
  exposedHeaders: ['Content-Type', 'Authorization', 'X-File-Format'],
  credentials: true,
  preflightContinue: false,
  optionsSuccessStatus: 204
}));

// 解析JSON请求体
app.use(express.json());

// 网络URL检查接口
app.post('/api/network/check-url', async (req, res) => {
  try {
    const { url } = req.body;
    if (!url) {
      return res.status(400).json({ 
        accessible: false, 
        error: '请提供URL' 
      });
    }

    console.log('正在检查URL:', url);

    const response = await axios.get(url, {
      timeout: 10000,
      validateStatus: false,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
      }
    });

    console.log('URL检查结果:', response.status);

    res.json({
      accessible: response.status >= 200 && response.status < 400,
      statusCode: response.status,
      error: response.status >= 400 ? `HTTP错误: ${response.status}` : null
    });
  } catch (error) {
    console.error('URL检查错误:', error.message);
    res.json({
      accessible: false,
      error: error.message
    });
  }
});

// 健康检查接口
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// 错误处理中间件
app.use((err, req, res, next) => {
  console.error('服务器错误:', err);
  res.status(500).json({
    error: '服务器内部错误',
    message: err.message
  });
});

// 添加OPTIONS请求的全局处理
app.options('*', cors());

const PORT = 3000;
app.listen(PORT, () => {
  console.log(`服务器运行在端口 ${PORT}`);
});
