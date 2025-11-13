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
    proxy: {
      '/vault': { target: 'http://localhost:8080', changeOrigin: true },
      '/fhe':   { target: 'http://localhost:8080', changeOrigin: true },
	}
  }
})
