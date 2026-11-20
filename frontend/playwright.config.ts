import { defineConfig } from '@playwright/test'

const backend = process.env.DAESOGO_E2E_BACKEND_URL

export default defineConfig({
  testDir: './tests',
  use: {
    baseURL: 'http://127.0.0.1:4175',
    channel: 'chrome',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: `npm run ${backend ? 'dev' : 'demo'} -- --host 127.0.0.1 --port 4175 --strictPort`,
    env: backend ? { DAESOGO_BACKEND_URL: backend } : { DAESOGO_FIXTURE_SEED: '00000000000000000000000000000059' },
    url: 'http://127.0.0.1:4175',
    reuseExistingServer: false,
  },
})
