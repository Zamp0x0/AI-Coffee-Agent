import { createRouter, createWebHistory } from 'vue-router'
import ChatInterface from '../components/ChatInterface.vue'
import OrderManagement from '../components/OrderManagement.vue'

const routes = [
  {
    path: '/',
    name: 'Chat',
    component: ChatInterface
  },
  {
    path: '/orders',
    name: 'OrderManagement',
    component: OrderManagement
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router