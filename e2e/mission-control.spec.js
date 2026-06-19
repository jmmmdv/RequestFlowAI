import { expect, test } from '@playwright/test';

test('creates a work item from the JavaScript dashboard', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Automation Mission Control' })).toBeVisible();

  await page.getByLabel('Title').fill('Learn Playwright locators');
  await page.getByLabel('Priority').selectOption('HIGH');
  await page.getByRole('button', { name: 'Add item' }).click();

  const card = page.locator('.card').filter({ hasText: 'Learn Playwright locators' });
  await expect(card).toContainText('HIGH');
});

test('shows the authenticated SaaS organization plan and quota usage', async ({ page }) => {
  await page.goto('/');

  await expect(page.locator('#organization-name')).toHaveText('Local Development');
  await expect(page.locator('#plan-badge')).toHaveText('FREE');
  await expect(page.locator('#usage-summary')).toContainText('/25 work items');
  await expect(page.locator('#usage-summary')).toContainText('/10 agent runs this month');
});

test('renders quota meters and offers an upgrade on the free plan', async ({ page }) => {
  await page.goto('/');

  await expect(page.locator('#usage-meters')).toBeVisible();
  await expect(page.locator('#work-items-figure')).toContainText('/ 25');
  await expect(page.locator('#agent-runs-figure')).toContainText('/ 10');
  await expect(page.locator('#saas-meta')).toContainText('ADMIN');
  await expect(page.getByRole('button', { name: 'Upgrade to Pro' })).toBeVisible();
});

test('lists members and creates a teammate invitation', async ({ page }) => {
  await page.goto('/');

  await expect(page.locator('#team-panel')).toBeVisible();
  await expect(page.locator('#members-list')).toContainText('ADMIN');

  await page.getByLabel('Email').fill('teammate@example.com');
  await page.getByLabel('Role').selectOption('MEMBER');
  await page.getByRole('button', { name: 'Send invite' }).click();

  await expect(page.locator('#invite-result')).toBeVisible();
  await expect(page.locator('#invite-token')).not.toBeEmpty();
  await expect(page.locator('#invitations-list')).toContainText('teammate@example.com');
});

test('agent decomposes a goal into three work items', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Goal').fill('Deploy the REST API to AWS');
  await page.getByRole('button', { name: 'Build plan' }).click();

  await expect(page.getByRole('status')).toContainText('created 3 work items');
  await expect(page.locator('.card').filter({ hasText: 'Deploy the REST API to AWS' })).toHaveCount(3);
  await expect(page.locator('.run-card').filter({ hasText: 'Deploy the REST API to AWS' })).toContainText('EXECUTED');
});

test('high-impact agent work requires approval before tools run', async ({ page }) => {
  await page.goto('/');
  const goal = `Fix urgent production outage ${Date.now()}`;
  await page.getByLabel('Goal').fill(goal);
  await page.getByRole('button', { name: 'Build plan' }).click();

  await expect(page.getByRole('status')).toContainText('waiting for human approval');
  await expect(page.locator('.card').filter({ hasText: goal })).toHaveCount(0);

  const audit = page.locator('.run-card').filter({ hasText: goal });
  await expect(audit).toContainText('PENDING APPROVAL');
  await audit.getByRole('button', { name: 'Approve & execute' }).click();

  await expect(page.locator('.run-card').filter({ hasText: goal })).toContainText('EXECUTED');
  await expect(page.locator('.card').filter({ hasText: goal })).toHaveCount(3);
});

test('agent API is idempotent for the same tenant and key', async ({ request }) => {
  const key = `playwright-${Date.now()}`;
  const data = { goal: 'Create an idempotent API test', createWorkItems: true, toolBudget: 3 };
  const first = await request.post('/api/agent/plan', { data, headers: { 'Idempotency-Key': key } });
  const second = await request.post('/api/agent/plan', { data, headers: { 'Idempotency-Key': key } });

  expect(first.status()).toBe(200);
  expect(second.status()).toBe(200);
  const firstBody = await first.json();
  const secondBody = await second.json();
  expect(secondBody.runId).toBe(firstBody.runId);
  expect(secondBody.createdWorkItemIds).toEqual(firstBody.createdWorkItemIds);
  expect(secondBody.outcome).toBe('EXECUTED');
  expect(second.headers()['idempotency-key']).toBe(key);
});

test('moves a work item through the board and updates the summary', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Title').fill('Ship observable API');
  await page.getByRole('button', { name: 'Add item' }).click();

  const card = page.locator('.card').filter({ hasText: 'Ship observable API' });
  await card.getByLabel('Work item status').selectOption('IN_PROGRESS');
  await expect(card.getByLabel('Work item status')).toHaveValue('IN_PROGRESS');
  await expect(page.locator('#active-count')).toHaveText('1');

  await card.getByLabel('Work item status').selectOption('DONE');
  await expect(page.locator('#done-count')).toHaveText('1');
});

test('REST API supports the full create-read-update-delete lifecycle', async ({ request }) => {
  const created = await request.post('/api/work-items', { data: {
    title: 'API exercise', description: 'CRUD through HTTP', priority: 'MEDIUM', status: 'BACKLOG',
  } });
  expect(created.status()).toBe(201);
  const item = await created.json();

  const updated = await request.put(`/api/work-items/${item.id}`, { data: {
    title: 'API exercise', description: 'Completed CRUD', priority: 'HIGH', status: 'DONE',
  } });
  expect((await updated.json()).status).toBe('DONE');

  expect((await request.delete(`/api/work-items/${item.id}`)).status()).toBe(204);
  expect((await request.get(`/api/work-items/${item.id}`)).status()).toBe(404);
});
