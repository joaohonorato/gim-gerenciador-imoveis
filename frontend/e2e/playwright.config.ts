import { defineConfig, devices } from '@playwright/test';

const disableWebServer = process.env.E2E_DISABLE_WEBSERVER === '1';

const webServer = disableWebServer
  ? undefined
  : [
      {
        cwd: '../backend',
        command: '.\\gradlew.bat run -x test',
        url: 'http://localhost:8080/auth/ping',
        reuseExistingServer: true,
        timeout: 120_000,
        stdout: 'pipe' as const,
      },
      {
        cwd: '..',
        command: 'npx expo start --web --port 19006',
        url: 'http://localhost:19006',
        reuseExistingServer: true,
        timeout: 120_000,
        stdout: 'pipe' as const,
      },
    ];

export default defineConfig({
  testDir: '.',
  testMatch: '**/*.spec.ts',
  fullyParallel: false,
  retries: 0,
  timeout: 60_000,
  use: {
    baseURL: 'http://localhost:19006',
    ...devices['Desktop Chrome'],
  },
  webServer,
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
