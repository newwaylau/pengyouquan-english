import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5175,
    host: '0.0.0.0',
    allowedHosts: ['english.pengyouquan.top'],
    proxy: {
      '/api': { target: 'http://localhost:8082', changeOrigin: true },
      '/audio': { target: 'http://localhost:8082', changeOrigin: true },
    },
  },
})
