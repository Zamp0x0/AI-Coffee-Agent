import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

/**
 * Vue3应用程序入口点
 * 创建并挂载Vue应用实例
 */
const app = createApp(App)

app.use(router)

// 挂载应用到DOM
app.mount('#app')