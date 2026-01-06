import { defineConfig } from 'vite'
import './polyfill.js';
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
          '/api/workflows': { target: 'http://127.0.0.1:8085', changeOrigin: true },
          '/api/order':     { target: 'http://127.0.0.1:8085', changeOrigin: true },
          '/api/user':      { target: 'http://127.0.0.1:8085', changeOrigin: true },
        }
  }
})