<template>
  <div class="app">
    <!-- 应用标题栏 -->
    <header class="app-header">
      <h1 class="app-title">AI咖啡Agent</h1>
      <nav>
        <router-link to="/">聊天</router-link>
        <router-link to="/orders" target="_blank">订单后台</router-link>
      </nav>
      <button @click="openRegisterModal" class="reset-btn">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"></polyline>
          <polyline points="1 20 1 14 7 14"></polyline>
          <path d="m3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
        </svg>
        新用户
      </button>
    </header>

    <!-- 路由出口 -->
    <router-view />

    <!-- 注册模态框 -->
    <RegisterModal :visible="isRegisterModalVisible" @close="closeRegisterModal" @registered="handleRegistered" />
  </div>
</template>

<script setup>
import { provide, ref } from 'vue';
import RegisterModal from './components/RegisterModal.vue';

/**
 * 应用根组件
 * 管理全局状态和用户重置功能
 */
 // 用户ID管理
let userId = ref("002");
let refreshMessage = ref(false);

const isRegisterModalVisible = ref(false);

const openRegisterModal = () => {
  // 触发聊天界面重置
  isRegisterModalVisible.value = true;
};

const closeRegisterModal = () => {
  isRegisterModalVisible.value = false;
};

const handleRegistered = (user) => {
  userId.value = user.id;
  console.log('新用户注册成功，用户ID:', userId.value);
  refreshMessage.value = true;
  // window.location.reload(); // 不再需要强制刷新页面
};

// 向子组件提供用户ID
provide('userId', userId)
provide('refreshMessage', refreshMessage)
</script>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

nav {
  display: flex;
  gap: 1rem;
}

nav a {
  color: white;
  text-decoration: none;
  font-size: 1rem;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

nav a.router-link-exact-active {
  background-color: rgba(255, 255, 255, 0.2);
}

.app-title {
  color: white;
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.reset-btn:active {
  transform: translateY(0);
}
</style>