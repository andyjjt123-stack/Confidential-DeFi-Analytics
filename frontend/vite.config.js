import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)), // @ -> /src
    },
  },
  server: {
    host: '0.0.0.0',                 // 對外開放（給 tunnel 用）
    port: 5173,                      // 你 cloudflared 連的那個 port
    allowedHosts: [
      'kids-northern-voices-arbitrary.trycloudflare.com', // cloudflared 給你的 host
    ],
    proxy: {
      '/vault': { target: 'http://localhost:8080', changeOrigin: true },
      '/fhe':   { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
