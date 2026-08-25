import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react({
      include: '**/*.{js,jsx}',
    }),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: process.env.VITE_API_BASE_URL || 'https://codesense-ai-tuo7.onrender.com',
        changeOrigin: true,
      }
    }
  }
})
