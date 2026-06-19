import { apiBaseUrl, authenticationConfigured, getAccessToken, initializeAuth, login, logout } from './auth.js';

const board = document.querySelector('#board');
const errorBox = document.querySelector('#error');
const runBoard = document.querySelector('#run-board');
const runError = document.querySelector('#run-error');
const demoMode = new URLSearchParams(location.search).has('demo') ||
  (location.hostname.endsWith('.vercel.app') && !authenticationConfigured());
const demoStateKey = 'mission-control-demo-v1';
const authState = await initializeAuth().catch((error) => ({ authenticated: false, configured: true, error }));
const loginButton = document.querySelector('#login');
const logoutButton = document.querySelector('#logout');

if (authState.configured && !demoMode) {
  loginButton.hidden = authState.authenticated;
  logoutButton.hidden = !authState.authenticated;
  document.querySelector('#account-status').textContent = authState.error
    ? authState.error.message : authState.authenticated ? 'Authenticated with Cognito' : 'Sign in to use the production API';
}
loginButton.addEventListener('click', login);
logoutButton.addEventListener('click', logout);

const initialDemoState = {
  nextItemId: 4,
  nextRunId: 2,
  workItems: [
    { id: 1, title: 'Protect every tenant boundary', description: 'JWT roles and tenant-scoped persistence', priority: 'CRITICAL', status: 'DONE' },
    { id: 2, title: 'Trace the planning agent', description: 'OpenTelemetry, Prometheus, Tempo, and Grafana', priority: 'HIGH', status: 'IN_PROGRESS' },
    { id: 3, title: 'Practice the restore runbook', description: 'Validate the RDS recovery objective', priority: 'MEDIUM', status: 'READY' },
  ],
  runs: [{
    id: 1,
    goal: 'Prepare a safe production deployment',
    classification: 'HIGH_IMPACT',
    outcome: 'PENDING_APPROVAL',
    toolBudget: 3,
    createdWorkItems: 0,
    userId: 'portfolio-viewer',
    correlationId: 'demo-correlation-001',
    createdAt: new Date().toISOString(),
  }],
};

function readDemoState() {
  const saved = localStorage.getItem(demoStateKey);
  return saved ? JSON.parse(saved) : structuredClone(initialDemoState);
}

function saveDemoState(state) {
  localStorage.setItem(demoStateKey, JSON.stringify(state));
}

async function demoApi(path, options = {}) {
  const method = options.method ?? 'GET';
  const state = readDemoState();
  if (path === '/api/work-items' && method === 'GET') {
    return { _embedded: { workItemList: state.workItems } };
  }
  if (path === '/api/work-items' && method === 'POST') {
    const request = JSON.parse(options.body);
    const item = { id: state.nextItemId++, ...request };
    state.workItems.push(item);
    saveDemoState(state);
    return item;
  }
  const statusMatch = path.match(/^\/api\/work-items\/(\d+)\/status$/);
  if (statusMatch && method === 'PATCH') {
    const item = state.workItems.find(({ id }) => id === Number(statusMatch[1]));
    if (item) item.status = JSON.parse(options.body).status;
    saveDemoState(state);
    return item;
  }
  const itemMatch = path.match(/^\/api\/work-items\/(\d+)$/);
  if (itemMatch && method === 'DELETE') {
    state.workItems = state.workItems.filter(({ id }) => id !== Number(itemMatch[1]));
    saveDemoState(state);
    return null;
  }
  if (path === '/api/agent/runs' && method === 'GET') return state.runs;
  if (path === '/api/saas/organization' && method === 'GET') return {
    id: 'demo', name: 'Portfolio Reviewers', slug: 'portfolio-reviewers', plan: 'PRO', status: 'ACTIVE',
    currentUserRole: 'ADMIN', billingConfigured: false, subscriptionStatus: 'DEMO',
    usage: { plan: 'PRO', workItemsUsed: state.workItems.length, workItemsLimit: 1000,
      agentRunsUsed: state.runs.length, agentRunsLimit: 500 },
  };
  if (path === '/api/billing/checkout' && method === 'POST') {
    throw new Error('Billing checkout is disabled in the browser-local portfolio demo.');
  }
  if (path === '/api/agent/plan' && method === 'POST') {
    const request = JSON.parse(options.body);
    const highImpact = /deploy|production|delete|security|database/i.test(request.goal);
    const createdWorkItemIds = [];
    const outcome = highImpact ? 'PENDING_APPROVAL' : request.createWorkItems ? 'EXECUTED' : 'DRY_RUN';
    if (outcome === 'EXECUTED') {
      const item = { id: state.nextItemId++, title: request.goal, description: 'Created safely by the portfolio demo agent', priority: 'MEDIUM', status: 'BACKLOG' };
      state.workItems.push(item);
      createdWorkItemIds.push(item.id);
    }
    const run = {
      id: state.nextRunId++, goal: request.goal, classification: highImpact ? 'HIGH_IMPACT' : 'ROUTINE',
      outcome, toolBudget: request.toolBudget, createdWorkItems: createdWorkItemIds.length,
      userId: 'portfolio-viewer', correlationId: crypto.randomUUID(), createdAt: new Date().toISOString(),
    };
    state.runs.unshift(run);
    saveDemoState(state);
    return { ...run, createdWorkItemIds };
  }
  const approvalMatch = path.match(/^\/api\/agent\/runs\/(\d+)\/approve$/);
  if (approvalMatch && method === 'POST') {
    const run = state.runs.find(({ id }) => id === Number(approvalMatch[1]));
    const item = { id: state.nextItemId++, title: run.goal, description: 'Created after explicit human approval', priority: 'HIGH', status: 'BACKLOG' };
    state.workItems.push(item);
    run.outcome = 'EXECUTED';
    run.createdWorkItems = 1;
    saveDemoState(state);
    return { ...run, createdWorkItemIds: [item.id] };
  }
  throw new Error('This action is unavailable in the portfolio demo.');
}

if (demoMode) document.querySelector('#demo-notice').hidden = false;

async function api(path, options = {}) {
  if (demoMode) return demoApi(path, options);
  const { headers = {}, ...requestOptions } = options;
  const token = getAccessToken();
  const response = await fetch(`${apiBaseUrl()}${path}`, {
    ...requestOptions,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
}

const planRank = { FREE: 0, PRO: 1, BUSINESS: 2 };

function renderMeter(prefix, used, limit) {
  const ratio = limit > 0 ? Math.min(used / limit, 1) : 0;
  const bar = document.querySelector(`#${prefix}-bar`);
  bar.style.width = `${Math.round(ratio * 100)}%`;
  bar.dataset.level = ratio >= 0.9 ? 'critical' : ratio >= 0.7 ? 'warning' : 'healthy';
  document.querySelector(`#${prefix}-figure`).textContent = `${used} / ${limit}`;
}

function renderUsage(organization) {
  const usage = organization.usage;
  renderMeter('work-items', usage.workItemsUsed, usage.workItemsLimit);
  renderMeter('agent-runs', usage.agentRunsUsed, usage.agentRunsLimit);
  document.querySelector('#usage-meters').hidden = false;

  const subscription = organization.subscriptionStatus && organization.subscriptionStatus !== 'ACTIVE'
    ? ` · subscription ${organization.subscriptionStatus.toLowerCase()}` : '';
  const meta = document.querySelector('#saas-meta');
  meta.textContent = `Signed in as ${organization.currentUserRole}` +
    `${organization.billingConfigured ? ' · billing connected' : ''}${subscription}`;
  meta.hidden = false;

  const note = document.querySelector('#plan-note');
  for (const button of document.querySelectorAll('.upgrade')) {
    button.hidden = planRank[button.dataset.plan] <= planRank[organization.plan];
  }
  const onTopPlan = planRank[organization.plan] >= planRank.BUSINESS;
  note.hidden = !onTopPlan;
  if (onTopPlan) note.textContent = 'You are on the top plan.';
}

async function loadOrganization() {
  if (authenticationConfigured() && !authState.authenticated && !demoMode) return;
  try {
    const organization = await api('/api/saas/organization');
    document.querySelector('#organization-name').textContent = organization.name;
    document.querySelector('#plan-badge').textContent = organization.plan;
    document.querySelector('#usage-summary').textContent =
      `${organization.usage.workItemsUsed}/${organization.usage.workItemsLimit} work items · ` +
      `${organization.usage.agentRunsUsed}/${organization.usage.agentRunsLimit} agent runs this month`;
    renderUsage(organization);
  } catch (error) {
    document.querySelector('#usage-summary').textContent = error.message;
    document.querySelector('#usage-meters').hidden = true;
    document.querySelector('#saas-meta').hidden = true;
  }
}

function showBillingReturn() {
  const params = new URLSearchParams(location.search);
  const outcome = params.get('billing');
  if (!outcome) return;
  const notice = document.querySelector('#billing-notice');
  notice.textContent = outcome === 'success'
    ? 'Payment confirmed. Your plan updates as soon as Stripe finishes processing.'
    : 'Checkout was cancelled. Your plan is unchanged.';
  notice.dataset.outcome = outcome;
  notice.hidden = false;
  params.delete('billing');
  params.delete('session_id');
  const query = params.toString();
  history.replaceState({}, '', location.pathname + (query ? `?${query}` : ''));
}

for (const button of document.querySelectorAll('.upgrade')) {
  button.addEventListener('click', async () => {
    button.disabled = true;
    try {
      const response = await api('/api/billing/checkout', { method: 'POST', body: JSON.stringify({
        plan: button.dataset.plan, idempotencyKey: crypto.randomUUID(),
      }) });
      location.assign(response.checkoutUrl);
    } catch (error) {
      document.querySelector('#usage-summary').textContent = error.message;
      button.disabled = false;
    }
  });
}

function extractItems(document) {
  return document._embedded?.workItemList ?? [];
}

async function loadItems() {
  errorBox.textContent = '';
  try {
    const document = await api('/api/work-items');
    render(extractItems(document));
  } catch (error) {
    errorBox.textContent = error.message;
  }
}

async function loadRuns() {
  runError.textContent = '';
  try {
    renderRuns(await api('/api/agent/runs'));
  } catch (error) {
    runError.textContent = error.message;
  }
}

function renderRuns(runs) {
  runBoard.replaceChildren();
  if (!runs.length) {
    runBoard.textContent = 'No agent decisions yet.';
    return;
  }
  for (const run of runs) {
    const view = document.querySelector('#run-template').content.cloneNode(true);
    const article = view.querySelector('article');
    article.dataset.runId = run.id;
    const outcome = view.querySelector('.outcome');
    outcome.textContent = run.outcome.replaceAll('_', ' ');
    outcome.dataset.outcome = run.outcome;
    view.querySelector('time').textContent = new Date(run.createdAt).toLocaleString();
    view.querySelector('h3').textContent = run.goal;
    view.querySelector('.run-meta').textContent = `${run.classification} · budget ${run.toolBudget} · ${run.createdWorkItems} tools used`;
    view.querySelector('.run-identity').textContent = `User ${run.userId} · correlation ${run.correlationId}`;
    const approve = view.querySelector('.approve');
    if (run.outcome === 'PENDING_APPROVAL') {
      approve.hidden = false;
      approve.addEventListener('click', () => approveRun(run.id, approve));
    }
    runBoard.append(view);
  }
}

async function approveRun(id, button) {
  button.disabled = true;
  try {
    const response = await api(`/api/agent/runs/${id}/approve`, { method: 'POST' });
    document.querySelector('#agent-result').textContent = `Approved: created ${response.createdWorkItemIds.length} work items.`;
    await Promise.all([loadItems(), loadRuns(), loadOrganization()]);
  } catch (error) {
    runError.textContent = error.message;
    button.disabled = false;
  }
}

function render(items) {
  document.querySelector('#total-count').textContent = items.length;
  document.querySelector('#active-count').textContent = items.filter((item) => item.status === 'IN_PROGRESS').length;
  document.querySelector('#done-count').textContent = items.filter((item) => item.status === 'DONE').length;
  board.replaceChildren();
  if (!items.length) {
    board.textContent = 'No work yet. Give the agent a mission.';
    return;
  }
  for (const item of items) {
    const card = document.querySelector('#card-template').content.cloneNode(true);
    card.querySelector('article').dataset.id = item.id;
    card.querySelector('h3').textContent = item.title;
    card.querySelector('.description').textContent = item.description || 'No description';
    card.querySelector('.priority').textContent = item.priority;
    card.querySelector('.priority').dataset.priority = item.priority;
    const status = card.querySelector('.status-select');
    status.value = item.status;
    status.addEventListener('change', () => changeStatus(item.id, status));
    card.querySelector('.delete').addEventListener('click', () => removeItem(item.id));
    board.append(card);
  }
}

async function changeStatus(id, control) {
  control.disabled = true;
  try {
    await api(`/api/work-items/${id}/status`, {
      method: 'PATCH', body: JSON.stringify({ status: control.value }),
    });
    await Promise.all([loadItems(), loadOrganization()]);
  } catch (error) {
    errorBox.textContent = error.message;
    await Promise.all([loadItems(), loadOrganization()]);
  } finally {
    control.disabled = false;
  }
}

async function removeItem(id) {
  try {
    await api(`/api/work-items/${id}`, { method: 'DELETE' });
    await Promise.all([loadItems(), loadOrganization()]);
  } catch (error) { errorBox.textContent = error.message; }
}

document.querySelector('#work-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const formElement = event.currentTarget;
  const form = new FormData(formElement);
  const submit = formElement.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    await api('/api/work-items', { method: 'POST', body: JSON.stringify({
      title: form.get('title'), description: 'Created from the JavaScript dashboard',
      priority: form.get('priority'), status: 'BACKLOG',
    }) });
    formElement.reset();
    await Promise.all([loadItems(), loadOrganization()]);
  } catch (error) { errorBox.textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#agent-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const result = document.querySelector('#agent-result');
  const submit = event.currentTarget.querySelector('button[type="submit"]');
  submit.disabled = true;
  result.textContent = 'Agent is planning…';
  try {
    const form = new FormData(event.currentTarget);
    const response = await api('/api/agent/plan', {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({
        goal: form.get('goal'), createWorkItems: form.has('createWorkItems'),
        toolBudget: Number(form.get('toolBudget')),
      }),
    });
    const messages = {
      EXECUTED: `Executed safely: created ${response.createdWorkItemIds.length} work items.`,
      PENDING_APPROVAL: 'Plan is ready and waiting for human approval.',
      DRY_RUN: 'Dry run complete: no tools were called.',
      BUDGET_EXCEEDED: 'Stopped safely: the tool-call budget is too small for this plan.',
      BLOCKED: 'Blocked by safety policy: no tools were called.',
    };
    result.textContent = `${response.classification}: ${messages[response.outcome]}`;
    await Promise.all([loadItems(), loadRuns(), loadOrganization()]);
  } catch (error) { result.textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#refresh').addEventListener('click', loadItems);
document.querySelector('#refresh-runs').addEventListener('click', loadRuns);
showBillingReturn();
Promise.all([loadItems(), loadRuns(), loadOrganization()]);
