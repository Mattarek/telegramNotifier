import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The Spring backend runs on :8080 - Vite forwards /api there, so no CORS config is needed
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
