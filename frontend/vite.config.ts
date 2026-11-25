import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev, /api is forwarded to the API gateway, so the browser sees a single origin (no CORS problem).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.VITE_GATEWAY_URL ?? 'http://localhost:8080', changeOrigin: true } },
  },
});
