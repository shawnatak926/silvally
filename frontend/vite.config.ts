import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react(), ...(mode === 'demo' ? [(await import('./demo/plugin.ts')).demoApi(Boolean(env.DAESOGO_FIXTURE_SEED))] : [])],
    server: {
      proxy: mode === 'demo' ? undefined : {
        '/api': { target: env.DAESOGO_BACKEND_URL || 'http://127.0.0.1:8080', changeOrigin: true },
      },
    },
  }
})
