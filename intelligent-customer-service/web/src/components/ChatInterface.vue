<template>
  <div class="chat-interface">
    <!-- 聊天消息历史区域 -->
    <div class="chat-history" ref="chatHistoryRef">
      <div class="messages-container">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-message">
          <div class="welcome-icon">👋</div>
          <h3>欢迎使用AI咖啡Agent</h3>
          <p>我是您的智能助手，有什么可以帮助您的吗？</p>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message', message.type]"
        >
          <div class="message-content">
            <div class="message-text">{{ message.text }}</div>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="isLoading" class="message assistant">
          <div class="message-content">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-container">
        <textarea
          v-model="inputMessage"
          @keydown="handleKeyDown"
          @input="adjustTextareaHeight"
          ref="textareaRef"
          placeholder="输入您的问题..."
          class="message-input"
          rows="1"
          :disabled="isLoading"
        ></textarea>
        <button
          @click="sendMessage"
          :disabled="!inputMessage.trim() || isLoading"
          class="send-button"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22,2 15,22 11,13 2,9"></polygon>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, inject, onMounted, watch } from 'vue'
import { sendWorkflowRequest } from '../services/api.js'

/**
 * 聊天界面组件
 * 负责消息显示、用户输入和API交互
 */

// 注入用户ID
const userId = inject('userId')
const refreshMessage = inject('refreshMessage')

watch(refreshMessage, (newValue, oldValue) => {
  console.log(`refreshMessage 从 ${oldValue} 变成了 ${newValue}`);
  if (newValue) {
    messages.value = []
    refreshMessage.value = false
  }
});

// 响应式数据
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)

// DOM引用
const chatHistoryRef = ref(null)
const textareaRef = ref(null)

/**
 * 发送消息
 * 处理用户输入并调用API
 */
const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value) return

  // 添加用户消息到历史
  const userMessage = {
    id: Date.now(),
    type: 'user',
    text: message,
    timestamp: new Date()
  }
  messages.value.push(userMessage)

  // 清空输入框
  inputMessage.value = ''
  resetTextareaHeight()

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 设置加载状态
  isLoading.value = true

  try {
    // 调用API
    const response = await sendWorkflowRequest({
      userId: userId.value, // 使用 .value 获取实际的 ID 字符串
      message: message
    })

    // 添加助手回复到历史
    const assistantMessage = {
      id: Date.now() + 1,
      type: 'assistant',
      text: response || '抱歉，我暂时无法回答您的问题。',
      timestamp: new Date()
    }
    messages.value.push(assistantMessage)

  } catch (error) {
    console.error('发送消息失败:', error)
    
    // 添加错误消息
    const errorMessage = {
      id: Date.now() + 1,
      type: 'assistant',
      text: '抱歉，服务暂时不可用，请稍后再试。',
      timestamp: new Date()
    }
    messages.value.push(errorMessage)
  } finally {
    isLoading.value = false
    await nextTick()
    scrollToBottom()
  }
}

/**
 * 处理键盘事件
 * 支持Enter发送，Shift+Enter换行
 */
const handleKeyDown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

/**
 * 自动调整文本框高度
 */
const adjustTextareaHeight = () => {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
  }
}

/**
 * 重置文本框高度
 */
const resetTextareaHeight = () => {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
  }
}

/**
 * 滚动到聊天历史底部
 */
const scrollToBottom = () => {
  const chatHistory = chatHistoryRef.value
  if (chatHistory) {
    chatHistory.scrollTop = chatHistory.scrollHeight
  }
}

/**
 * 格式化时间显示
 */
const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 组件挂载后聚焦输入框
onMounted(() => {
  textareaRef.value?.focus()
})
</script>

<style scoped>
.chat-interface {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  scroll-behavior: smooth;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 100%;
}

.welcome-message {
  text-align: center;
  padding: 2rem;
  color: #666;
  margin-top: auto;
  margin-bottom: auto;
}

.welcome-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.welcome-message h3 {
  margin-bottom: 0.5rem;
  color: #333;
}

.message {
  display: flex;
  margin-bottom: 1rem;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 0.75rem 1rem;
  border-radius: 18px;
  position: relative;
}

.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-content {
  background: #f1f3f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-text {
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
  margin-top: 0.25rem;
  text-align: right;
}

.message.assistant .message-time {
  text-align: left;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input-area {
  padding: 1rem;
  background: white;
  border-top: 1px solid #e9ecef;
}

.input-container {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  max-width: 100%;
}

.message-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: 2px solid #e9ecef;
  border-radius: 20px;
  font-size: 1rem;
  font-family: inherit;
  resize: none;
  outline: none;
  transition: border-color 0.2s ease;
  min-height: 44px;
  max-height: 120px;
  line-height: 1.5;
}

.message-input:focus {
  border-color: #667eea;
}

.message-input:disabled {
  background: #f8f9fa;
  cursor: not-allowed;
}

.send-button {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 滚动条样式 */
.chat-history::-webkit-scrollbar {
  width: 6px;
}

.chat-history::-webkit-scrollbar-track {
  background: transparent;
}

.chat-history::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.chat-history::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-interface {
    border-radius: 0;
    height: 100vh;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .chat-history {
    padding: 0.75rem;
  }
  
  .chat-input-area {
    padding: 0.75rem;
  }
}
</style>