import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  root: 'src/web',
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.BACKEND_URL ?? 'http://127.0.0.1:3001',
      '/ws': { target: process.env.BACKEND_URL ?? 'http://127.0.0.1:3001', ws: true },
    },
  },
  build: { outDir: '../../dist/web', emptyOutDir: false },
});
