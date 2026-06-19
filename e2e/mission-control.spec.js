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

test('agent decomposes a goal into three work items', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Goal').fill('Deploy the REST API to AWS');
  await page.getByRole('button', { name: 'Build plan' }).click();

  await expect(page.getByRole('status')).toContainText('created 3 work items');
  await expect(page.locator('.card').filter({ hasText: 'Deploy the REST API to AWS' })).toHaveCount(3);
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
