import { expect, test } from '@playwright/test';

test('shows the customer journey and honest pricing', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle('RequestFlow AI — Turn requests into trackable work');
  await expect(page.getByRole('heading', { name: 'Never lose another customer request' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'One simple home for incoming work' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'From scattered messages to clear next steps' })).toBeVisible();
  await expect(page.locator('.workflow-grid article')).toHaveCount(5);
  await expect(page.locator('#benefits')).toContainText('Fewer lost requests');
  await expect(page.locator('#benefits')).toContainText('Simple approvals');
  await expect(page.locator('#pricing')).toContainText('FREE');
  await expect(page.locator('#pricing')).toContainText('Pilot pricing coming soon');
  await expect(page.getByRole('heading', { name: /Send a request to/ })).toContainText('Local Development');
});

test('keeps the working dashboard accessible from the landing page', async ({ page }) => {
  await page.goto('/');

  await page.locator('header').getByRole('link', { name: 'View demo' }).click();
  await expect(page).toHaveURL(/#workspace$/);
  await expect(page.getByRole('heading', { name: 'Keep every request moving' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Trackable work' })).toBeVisible();
});

test('public intake classifies a request and adds it to the workspace', async ({ page }) => {
  await page.goto('/');
  const title = `Booking form outage ${Date.now()}`;

  const intake = page.locator('#intake-form');
  await intake.getByLabel('Your name').fill('Taylor Client');
  await intake.getByLabel('Email').fill('taylor@example.com');
  await intake.getByLabel('Company or client name').fill('Taylor Studio');
  await intake.getByLabel('What do you need?').fill(title);
  await intake.getByLabel('Details').fill('Urgent: customers cannot access the booking form today.');
  await intake.getByLabel('Category Optional').selectOption('SUPPORT');
  await intake.getByLabel('Urgency Optional').selectOption('URGENT');
  await intake.getByRole('button', { name: 'Send request' }).click();

  await expect(page.locator('#intake-result')).toContainText('Request received · Support · CRITICAL priority');
  const requestCard = page.locator('.request-card').filter({ hasText: title });
  await expect(requestCard).toContainText('Taylor Client · Taylor Studio');
  await expect(requestCard).toContainText('NEW');
  await expect(page.locator('.card').filter({ hasText: title })).toContainText('CRITICAL');
});

test('client uses a dedicated shared form and the owner sees the request', async ({ page }) => {
  const title = `Client campaign request ${Date.now()}`;
  await page.goto('/public-request.html?organization=local');

  await expect(page.getByRole('heading', { name: 'Send a request to Local Development' })).toBeVisible();
  const form = page.locator('#public-request-form');
  await form.getByLabel('Your name').fill('Jordan Client');
  await form.getByLabel('Your email').fill('jordan@example.com');
  await form.getByLabel('Company or client name').fill('Jordan Consulting');
  await form.getByLabel('Request title').fill(title);
  await form.getByLabel('Request details').fill('Please update our campaign landing page before next Friday.');
  await form.getByLabel('Category Optional').selectOption('CHANGE_REQUEST');
  await form.getByLabel('Urgency Optional').selectOption('HIGH');
  await form.getByRole('button', { name: 'Send request' }).click();

  await expect(page.getByRole('heading', { name: 'Your request is in' })).toBeVisible();
  await expect(page.locator('#confirmation-reference')).toHaveText(/^RF-[A-F0-9]{8}$/);
  await expect(page.locator('#confirmation-status')).toHaveText('New');
  await expect(page.locator('#confirmation-priority')).toHaveText('High');

  await page.goto('/#workspace');
  const ownerRequest = page.locator('.request-card').filter({ hasText: title });
  await expect(ownerRequest).toContainText('Jordan Consulting');
  await expect(ownerRequest).toContainText('requested Change Request');
  await expect(page.locator('.card').filter({ hasText: title })).toContainText('HIGH');
});

test('creates a work item from the JavaScript dashboard', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Never lose another customer request' })).toBeVisible();

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
  await expect(page.locator('#usage-summary')).toContainText('/10 assisted plans this month');
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

  const inviteForm = page.locator('#invite-form');
  await inviteForm.getByLabel('Email').fill('teammate@example.com');
  await inviteForm.getByLabel('Role').selectOption('MEMBER');
  await inviteForm.getByRole('button', { name: 'Send invite' }).click();

  await expect(page.locator('#invite-result')).toBeVisible();
  await expect(page.locator('#invite-token')).not.toBeEmpty();
  await expect(page.locator('#invitations-list')).toContainText('teammate@example.com');
});

test('accepts an invitation with its one-time token', async ({ page }) => {
  await page.goto('/');

  // Invite the current user's own email so acceptance is permitted.
  const inviteForm = page.locator('#invite-form');
  await inviteForm.getByLabel('Email').fill('developer@local.test');
  await inviteForm.getByLabel('Role').selectOption('MEMBER');
  await inviteForm.getByRole('button', { name: 'Send invite' }).click();
  await expect(page.locator('#invite-token')).not.toBeEmpty();
  const token = await page.locator('#invite-token').textContent();

  await page.getByLabel('Have an invitation? Paste your token').fill(token.trim());
  await page.getByRole('button', { name: 'Accept invitation' }).click();

  await expect(page.locator('#accept-result')).toContainText('Joined');
});

test('agent decomposes a goal into three work items', async ({ page }) => {
  await page.goto('/');
  await page.locator('#goal').fill('Prepare the client campaign brief');
  await page.getByRole('button', { name: 'Create work plan' }).click();

  await expect(page.getByRole('status')).toContainText('created 3 work items');
  await expect(page.locator('.card').filter({ hasText: 'Prepare the client campaign brief' })).toHaveCount(3);
  await expect(page.locator('.run-card').filter({ hasText: 'Prepare the client campaign brief' })).toContainText('EXECUTED');
});

test('high-impact agent work requires approval before tools run', async ({ page }) => {
  await page.goto('/');
  const goal = `Urgent production outage for the client portal ${Date.now()}`;
  await page.locator('#goal').fill(goal);
  await page.getByRole('button', { name: 'Create work plan' }).click();

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
