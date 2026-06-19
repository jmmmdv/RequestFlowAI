import { defineConfig, devices } from '@playwright/test';

const port = process.env.PORT ?? '8080';
const baseURL = `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: './e2e',
  // These journeys intentionally share one stateful application and database.
  // Serial execution keeps summary assertions deterministic instead of allowing
  // another test to create or complete work while a journey is in progress.
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 2 : 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{
    name: 'chromium',
    use: { ...devices['Desktop Chrome'], channel: process.env.CI ? undefined : 'chrome' },
  }],
  webServer: {
    command: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=${port}`,
    url: `${baseURL}/actuator/health`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
