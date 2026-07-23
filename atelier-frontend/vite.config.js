import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react({
    jsxRuntime: 'automatic',
  })],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ["./src/setupTests.js"],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      all: true,
    },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
    }
  }
});
