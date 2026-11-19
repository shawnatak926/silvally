import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(async ({ mode, command }) => {
  const env = loadEnv(mode, process.cwd(), '')
  if (command === 'build' && process.env.VERCEL === '1') {
    let api: URL | undefined
    try { api = new URL(env.VITE_API_BASE_URL) } catch { /* Validated below. */ }
    if (!api || api.protocol !== 'https:' || ['localhost', '127.0.0.1', '[::1]'].includes(api.hostname)
      || !api.pathname.replace(/\/$/, '').endsWith('/api/v1') || api.search || api.hash || api.username || api.password) {
      throw new Error('Vercel 환경변수 VITE_API_BASE_URL에 배포된 HTTPS Spring 서버 주소를 /api/v1까지 입력해 주세요.')
    }
  }
  return {
    define: { 'import.meta.env.VITE_GAME_EDITION': JSON.stringify(mode === 'mild' || mode === 'spicy' ? mode.toUpperCase() : undefined) },
    build: { outDir: mode === 'mild' || mode === 'spicy' ? `dist/${mode}` : 'dist', emptyOutDir: true },
    plugins: [react(), ...(mode === 'demo' ? [(await import('./demo/plugin.ts')).demoApi(Boolean(env.DAESOGO_FIXTURE_SEED))] : [])],
    server: {
      proxy: mode === 'demo' ? undefined : {
        '/api': { target: env.DAESOGO_BACKEND_URL || 'http://127.0.0.1:8080', changeOrigin: true },
      },
    },
  }
})
