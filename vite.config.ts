import angular from '@analogjs/vite-plugin-angular';
import {playwright} from '@vitest/browser-playwright';
import {defineConfig} from 'vitest/config';

export default defineConfig(({mode}) => ({
  plugins: [angular({tsconfig: 'tsconfig.spec.json'})],
  test: {
    globals: true,
    include: ['src/**/*.spec.ts'],
    setupFiles: ['src/test-setup.ts'],
    environment: mode === 'browser' ? 'node' : 'jsdom',
    browser: mode === 'browser' ? {
      enabled: true,
      headless: true,
      provider: playwright(),
      instances: [{browser: 'chromium'}]
    } : undefined
  }
}));
