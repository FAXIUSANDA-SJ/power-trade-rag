<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <el-icon><ChatLineRound /></el-icon>
          <span>电力交易智能问答</span>
        </div>
      </template>
      
      <div class="messages-container" ref="messagesContainer">
        <div 
          v-for="(message, index) in messages" 
          :key="index" 
          :class="['message', message.role === 'user' ? 'user-message' : 'bot-message']"
        >
          <div class="message-content">
            <p>{{ message.content }}</p>
          </div>
          <div class="message-time">{{ message.time }}</div>
        </div>
      </div>
      
      <div class="input-container">
        <el-row :gutter="10">
          <el-col :span="20">
            <el-input
              v-model="userInput"
              placeholder="请输入您的问题，例如：什么是电力中长期交易？"
              @keyup.enter="sendMessage"
              :disabled="isLoading"
              clearable
            />
          </el-col>
          <el-col :span="4">
            <el-button 
              type="primary" 
              @click="sendMessage" 
              :loading="isLoading"
              style="width: 100%"
            >
              发送
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { chatApi } from '../api/chat'

const userInput = ref('')
const messages = ref([])
const isLoading = ref(false)
const sessionId = ref(null)

const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return
  
  const userMessage = {
    role: 'user',
    content: userInput.value,
    time: formatTime(new Date())
  }
  
  messages.value.push(userMessage)
  const query = userInput.value
  userInput.value = ''
  
  isLoading.value = true
  
  try {
    const response = await chatApi.ask({
      sessionId: sessionId.value,
      query: query
    })
    
    const botMessage = {
      role: 'bot',
      content: response.data.answer || '抱歉，暂时无法回答您的问题',
      time: formatTime(new Date())
    }
    
    messages.value.push(botMessage)
    sessionId.value = response.data.sessionId
  } catch (error) {
    console.error('请求失败:', error)
    const errorMessage = {
      role: 'bot',
      content: '系统错误，请稍后重试',
      time: formatTime(new Date())
    }
    messages.value.push(errorMessage)
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const formatTime = (date) => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = () => {
  nextTick(() => {
    const container = document.querySelector('.messages-container')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  height: calc(100vh - 140px);
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 10px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

.message {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  align-items: flex-end;
}

.bot-message {
  align-items: flex-start;
}

.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px 12px 0 12px;
}

.bot-message .message-content {
  background: white;
  color: #303133;
  border-radius: 12px 12px 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message-content {
  max-width: 70%;
  padding: 15px 20px;
  line-height: 1.6;
  word-break: break-word;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.input-container {
  padding: 20px;
  background: white;
  border-top: 1px solid #e4e7ed;
}
</style>
