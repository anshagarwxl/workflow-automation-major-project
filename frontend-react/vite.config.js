import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the portal proxies /api to the Spring Boot backend; in Docker, nginx does the same.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_BACKEND_URL || 'http://localhost:8081', changeOrigin: true },
      '/swagger-ui': { target: process.env.VITE_BACKEND_URL || 'http://localhost:8081', changeOrigin: true },
      '/v3/api-docs': { target: process.env.VITE_BACKEND_URL || 'http://localhost:8081', changeOrigin: true },
    },
  },
  build: {
    chunkSizeWarningLimit: 1500,
  },
});
