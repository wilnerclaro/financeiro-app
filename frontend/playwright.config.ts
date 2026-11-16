import { defineConfig, devices } from '@playwright/test';

const emCi = !!process.env['CI'];
const launchOptions = { executablePath: process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE'] };

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: emCi,
  retries: emCi ? 1 : 0,
  reporter: emCi ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4300',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], launchOptions } },
    { name: 'celular', use: { ...devices['Pixel 7'], launchOptions } },
  ],
  webServer: {
    command: 'npm start -- --port 4300',
    url: 'http://localhost:4300',
    reuseExistingServer: !emCi,
    timeout: 120_000,
  },
});
