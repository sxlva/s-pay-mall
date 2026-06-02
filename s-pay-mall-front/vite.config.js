import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    proxy: {
      // 将 /mall-api 代理到后端服务
      '/mall-api': {
        target: 'http://localhost:8092',
        changeOrigin: true,
        // 不需要 rewrite，因为后端已经包含 /mall-api 前缀
      },
      // 将 /pay-api 代理到后端服务
      '/pay-api': {
        target: 'http://localhost:8092',
        changeOrigin: true,
      },
    }
  }
})
