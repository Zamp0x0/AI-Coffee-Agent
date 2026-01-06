<template>
  <div class="modal-overlay" v-if="visible">
    <div class="modal-container">
      <div class="modal-header">
        <h2>新用户注册</h2>
      </div>
      <div class="modal-body">
        <form @submit.prevent="submitForm">
          <div class="form-group">
            <label for="name">用户名</label>
            <input type="text" id="name" v-model="name" required>
          </div>
          <div class="form-group">
            <label for="phone">手机号</label>
            <input type="text" id="phone" v-model="phone" required>
          </div>
          <div class="form-actions">
            <button type="submit">注册</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { registerUser } from '../services/api';

const props = defineProps({
  visible: Boolean,
});

const emit = defineEmits(['close', 'registered']);

const name = ref('');
const phone = ref('');

const closeModal = () => {
  name.value = '';
  phone.value = '';
  emit('close');

};

const submitForm = async () => {
  if (name.value && phone.value) {
    try {
      const user = await registerUser({ name: name.value, phone: phone.value });
      emit('registered', user);
      closeModal();
    } catch (error) {
      console.error('注册失败:', error);
      // 在这里可以添加错误提示
    }
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-container {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}

.modal-body {
  padding-top: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
}
</style>