<template>
  <div class="order-management">
    <header class="page-header">
      <h1>订单后台</h1>
      <button @click="fetchOrders()" class="action-btn complete-btn">刷新</button>
    </header>

    <main class="order-list-container">
      <table class="order-table">
        <thead>
          <tr>
            <th>订单ID</th>
            <th>商品名称</th>
            <th>用户姓名</th>
            <th>用户手机号</th>
            <th>价格</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="isLoading">
            <td colspan="6" class="loading-cell">加载中...</td>
          </tr>
          <tr v-else-if="filteredOrders.length === 0">
            <td colspan="6" class="no-data-cell">没有找到订单</td>
          </tr>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.itemName }}</td>
            <td>{{ order.userName }}</td>
            <td>{{ order.userPhone }}</td>
            <td>{{ order.price }}</td>
            <td>{{ order.status == 0 ? '制作中' : order.status == 1 ? '已完成' : '已取消' }}</td>
            <td>{{ new Date(order.createTime).toLocaleString() }}</td>
            <td class="actions-cell">
              <button @click="completeOrderAction(order.orderNumber)" :disabled="order.status != 0" class="action-btn complete-btn">完成</button>
              <button @click="cancelOrderAction(order.orderNumber)" :disabled="order.status != 0" class="action-btn cancel-btn">取消</button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getOrderList, completeOrder, cancelOrder } from '../services/api.js'

const orders = ref([])
const searchQuery = ref('')
const isLoading = ref(false)

const fetchOrders = async () => {
  isLoading.value = true
  try {
    orders.value = await getOrderList()
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchOrders)

const filteredOrders = computed(() => {
  if (!searchQuery.value) {
    return orders.value
  }
  return orders.value.filter(order =>
    Object.values(order).some(value =>
      String(value).toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  )
})

const completeOrderAction = async (orderId) => {
  try {
    await completeOrder(orderId)
    await fetchOrders() // Refresh the list
  } catch (error) {
    console.error(`完成订单 ${orderId} 失败:`, error)
  }
}

const cancelOrderAction = async (orderId) => {
  try {
    await cancelOrder(orderId)
    await fetchOrders() // Refresh the list
  } catch (error) {
    console.error(`取消订单 ${orderId} 失败:`, error)
  }
}

const filterOrders = () => {
  // This function is called on input to trigger the computed property recalculation
}
</script>

<style scoped>
.order-management {
  padding: 2rem;
  background-color: #f9fafb;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

h1 {
  font-size: 2rem;
  font-weight: 600;
  color: #333;
}

.search-container input {
  padding: 0.5rem 1rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  font-size: 1rem;
}

.order-list-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.order-table {
  width: 100%;
  border-collapse: collapse;
}

.order-table th,
.order-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.order-table th {
  background-color: #f9fafb;
  font-weight: 600;
  color: #666;
}

.loading-cell,
.no-data-cell {
  text-align: center;
  padding: 2rem;
  color: #999;
}

.actions-cell {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  padding: 0.25rem 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s ease;
}

.complete-btn {
  background-color: #28a745;
  color: white;
}

.cancel-btn {
  background-color: #dc3545;
  color: white;
}

.action-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>