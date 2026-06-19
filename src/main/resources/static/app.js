import { apiBaseUrl, authenticationConfigured, getAccessToken, initializeAuth, login, logout, publicOrganizationSlug } from './auth.js';

const board = document.querySelector('#board');
const errorBox = document.querySelector('#error');
const runBoard = document.querySelector('#run-board');
const runError = document.querySelector('#run-error');
const requestBoard = document.querySelector('#request-board');
const requestError = document.querySelector('#request-error');
const queryParameters = new URLSearchParams(location.search);
const intakeSlug = queryParameters.get('organization') || publicOrganizationSlug();
const demoMode = queryParameters.has('demo') ||
  (location.hostname.endsWith('.vercel.app') && !authenticationConfigured());
const demoStateKey = 'requestflow-ai-demo-v1';
const legacyDemoStateKey = 'mission-control-demo-v1';
const authState = await initializeAuth().catch((error) => ({ authenticated: false, configured: true, error }));
const loginButton = document.querySelector('#login');
const logoutButton = document.querySelector('#logout');
const workspaceAvailable = !authenticationConfigured() || authState.authenticated || demoMode;

if (authState.configured && !demoMode) {
  loginButton.hidden = authState.authenticated;
  logoutButton.hidden = !authState.authenticated;
  document.querySelector('#account-status').textContent = authState.error
    ? authState.error.message : authState.authenticated ? 'Authenticated with Cognito' : 'Sign in to use the production API';
}
loginButton.addEventListener('click', login);
logoutButton.addEventListener('click', logout);
document.querySelector('#start-free').addEventListener('click', () => {
  if (authenticationConfigured() && !authState.authenticated && !demoMode) return login();
  document.querySelector('#workspace').scrollIntoView({ behavior: 'smooth' });
});
if (!workspaceAvailable) {
  for (const panel of document.querySelectorAll('.workspace-panel')) panel.hidden = true;
}

const initialDemoState = {
  nextItemId: 4,
  nextRunId: 2,
  workItems: [
    { id: 1, title: 'Confirm the urgent website fix', description: 'Client reported that the booking form is unavailable', priority: 'CRITICAL', status: 'DONE' },
    { id: 2, title: 'Prepare the June campaign brief', description: 'Turn the client request into clear deliverables', priority: 'HIGH', status: 'IN_PROGRESS' },
    { id: 3, title: 'Schedule the support follow-up', description: 'Reply to the customer with the next available time', priority: 'MEDIUM', status: 'READY' },
  ],
  runs: [{
    id: 1,
    goal: 'Urgent: restore the client booking form',
    classification: 'HIGH_IMPACT',
    outcome: 'PENDING_APPROVAL',
    toolBudget: 3,
    createdWorkItems: 0,
    userId: 'demo-admin',
    correlationId: 'demo-correlation-001',
    createdAt: new Date().toISOString(),
  }],
  members: [
    { id: 'demo-member-1', tenantId: 'demo', userId: 'demo-admin', email: 'owner@example.com', role: 'ADMIN', joinedAt: new Date().toISOString() },
    { id: 'demo-member-2', tenantId: 'demo', userId: 'demo-teammate', email: 'teammate@example.com', role: 'MEMBER', joinedAt: new Date().toISOString() },
  ],
  invitations: [
    { id: 'demo-invite-1', tenantId: 'demo', email: 'teammate@example.com', role: 'VIEWER', invitedBy: 'demo-admin', expiresAt: new Date(Date.now() + 6 * 86400000).toISOString(), acceptedAt: null, createdAt: new Date().toISOString(), token: 'demo-seed-invitation-token-000000000000' },
  ],
  requests: [{
    id: 'demo-request-1', requesterName: 'Morgan Client', requesterEmail: 'morgan@example.com',
    title: 'Booking form stops at payment', details: 'Customers cannot finish a booking after entering payment details.',
    category: 'SUPPORT', suggestedPriority: 'HIGH', status: 'RECEIVED', workItemId: 1,
    internalSummary: 'Booking form stops at payment — Customers cannot finish a booking after entering payment details.',
    recommendedNextAction: 'Confirm the impact, reproduce the issue, and send the requester a status update.',
    createdAt: new Date().toISOString(),
  }],
};

function readDemoState() {
  const saved = localStorage.getItem(demoStateKey) ?? localStorage.getItem(legacyDemoStateKey);
  if (!saved) return structuredClone(initialDemoState);
  const parsed = JSON.parse(saved);
  return { ...structuredClone(initialDemoState), ...parsed, requests: parsed.requests ?? [] };
}

function saveDemoState(state) {
  localStorage.setItem(demoStateKey, JSON.stringify(state));
  localStorage.removeItem(legacyDemoStateKey);
}

function demoClassification(title, details) {
  const text = `${title} ${details}`.toLowerCase();
  const category = /invoice|billing|payment|refund|charge/.test(text) ? 'BILLING'
    : /bug|broken|error|outage|down|not working|cannot/.test(text) ? 'SUPPORT'
      : /quote|pricing|proposal|estimate|sales/.test(text) ? 'SALES'
        : /change|update|add|build|design|campaign/.test(text) ? 'CHANGE_REQUEST' : 'GENERAL';
  const suggestedPriority = /urgent|emergency|outage|down|critical/.test(text) ? 'CRITICAL'
    : /asap|today|blocked|deadline/.test(text) ? 'HIGH'
      : /no rush|when available|question/.test(text) ? 'LOW' : 'MEDIUM';
  const actions = {
    SUPPORT: 'Confirm the impact, reproduce the issue, and send the requester a status update.',
    BILLING: 'Review the account and transaction history before replying with the resolution path.',
    SALES: 'Confirm the requested outcome, budget, and timing, then assign a follow-up owner.',
    CHANGE_REQUEST: 'Clarify scope and acceptance criteria before estimating and scheduling the work.',
    GENERAL: 'Confirm the desired outcome and assign the request to the right team member.',
  };
  return { category, suggestedPriority, internalSummary: `${title} — ${details.slice(0, 240)}`,
    recommendedNextAction: actions[category] };
}

async function demoApi(path, options = {}) {
  const method = options.method ?? 'GET';
  const state = readDemoState();
  const publicIntakeMatch = path.match(/^\/api\/public\/intake\/([a-z0-9-]+)$/);
  if (publicIntakeMatch && method === 'GET') {
    return { organizationName: 'Brightside Services', organizationSlug: publicIntakeMatch[1] };
  }
  if (publicIntakeMatch && method === 'POST') {
    const request = JSON.parse(options.body);
    const classification = demoClassification(request.title, request.details);
    const workItemId = state.nextItemId++;
    state.workItems.push({ id: workItemId, title: request.title,
      description: classification.internalSummary, priority: classification.suggestedPriority, status: 'BACKLOG' });
    const submission = { id: crypto.randomUUID(), ...request, ...classification, status: 'RECEIVED',
      workItemId, createdAt: new Date().toISOString() };
    state.requests.unshift(submission);
    saveDemoState(state);
    return { requestId: submission.id, category: classification.category,
      suggestedPriority: classification.suggestedPriority,
      recommendedNextAction: classification.recommendedNextAction,
      receivedAt: submission.createdAt, replayed: false };
  }
  if (path === '/api/requests' && method === 'GET') return state.requests;
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
    id: 'demo', name: 'Brightside Services', slug: 'brightside-services', plan: 'PRO', status: 'ACTIVE',
    currentUserRole: 'ADMIN', billingConfigured: false, subscriptionStatus: 'DEMO',
    usage: { plan: 'PRO', workItemsUsed: state.workItems.length, workItemsLimit: 1000,
      agentRunsUsed: state.runs.length, agentRunsLimit: 500 },
  };
  if (path === '/api/billing/checkout' && method === 'POST') {
    throw new Error('Billing checkout is disabled in the browser-local pilot demo.');
  }
  if (path === '/api/saas/members' && method === 'GET') return state.members;
  if (path === '/api/saas/invitations' && method === 'GET') return state.invitations;
  if (path === '/api/saas/invitations' && method === 'POST') {
    const request = JSON.parse(options.body);
    const invitation = {
      id: crypto.randomUUID(), tenantId: 'demo', email: request.email, role: request.role,
      invitedBy: 'demo-admin', expiresAt: new Date(Date.now() + 7 * 86400000).toISOString(),
      acceptedAt: null, createdAt: new Date().toISOString(), token: `demo-${crypto.randomUUID().replaceAll('-', '')}`,
    };
    state.invitations.unshift(invitation);
    saveDemoState(state);
    return { invitationId: invitation.id, email: invitation.email, role: invitation.role,
      expiresAt: invitation.expiresAt, token: invitation.token };
  }
  if (path === '/api/saas/invitations/accept' && method === 'POST') {
    const { token } = JSON.parse(options.body);
    const invitation = state.invitations.find((entry) => entry.token === token && !entry.acceptedAt);
    if (!invitation) throw new Error('Invitation has expired or was already accepted.');
    invitation.acceptedAt = new Date().toISOString();
    if (!state.members.some((member) => member.email === invitation.email)) {
      state.members.push({ id: crypto.randomUUID(), tenantId: 'demo', userId: `invited-${invitation.email}`,
        email: invitation.email, role: invitation.role, joinedAt: new Date().toISOString() });
    }
    saveDemoState(state);
    return { id: 'demo', name: 'Brightside Services', slug: 'brightside-services', plan: 'PRO', status: 'ACTIVE',
      currentUserRole: 'ADMIN', billingConfigured: false, subscriptionStatus: 'DEMO',
      usage: { plan: 'PRO', workItemsUsed: state.workItems.length, workItemsLimit: 1000,
        agentRunsUsed: state.runs.length, agentRunsLimit: 500 } };
  }
  if (path === '/api/agent/plan' && method === 'POST') {
    const request = JSON.parse(options.body);
    const highImpact = /deploy|production|delete|security|database/i.test(request.goal);
    const createdWorkItemIds = [];
    const outcome = highImpact ? 'PENDING_APPROVAL' : request.createWorkItems ? 'EXECUTED' : 'DRY_RUN';
    if (outcome === 'EXECUTED') {
      const item = { id: state.nextItemId++, title: request.goal, description: 'Created by the rule-based request assistant', priority: 'MEDIUM', status: 'BACKLOG' };
      state.workItems.push(item);
      createdWorkItemIds.push(item.id);
    }
    const run = {
      id: state.nextRunId++, goal: request.goal, classification: highImpact ? 'HIGH_IMPACT' : 'ROUTINE',
      outcome, toolBudget: request.toolBudget, createdWorkItems: createdWorkItemIds.length,
      userId: 'demo-admin', correlationId: crypto.randomUUID(), createdAt: new Date().toISOString(),
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
  throw new Error('This action is unavailable in the browser-local pilot demo.');
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
const classificationLabels = {
  QUALITY_ENGINEERING: 'Quality or issue',
  DELIVERY_AUTOMATION: 'Delivery or release',
  SOFTWARE_DEVELOPMENT: 'Technical request',
  INTELLIGENT_AUTOMATION: 'General request',
  HIGH_IMPACT: 'High-impact request',
  ROUTINE: 'Routine request',
};

function classificationLabel(value) {
  return classificationLabels[value] ?? value.replaceAll('_', ' ').toLowerCase();
}

function friendlyCategory(value) {
  return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
}

async function loadIntakePortal() {
  try {
    const portal = await api(`/api/public/intake/${encodeURIComponent(intakeSlug)}`);
    document.querySelector('#intake-organization').textContent = portal.organizationName;
  } catch (error) {
    document.querySelector('#intake-description').textContent = error.message;
    document.querySelector('#intake-form').hidden = true;
  }
}

document.querySelector('#intake-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const formElement = event.currentTarget;
  const form = new FormData(formElement);
  const submit = formElement.querySelector('button[type="submit"]');
  const result = document.querySelector('#intake-result');
  const error = document.querySelector('#intake-error');
  submit.disabled = true;
  error.textContent = '';
  result.hidden = true;
  try {
    const receipt = await api(`/api/public/intake/${encodeURIComponent(intakeSlug)}`, {
      method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify({
        requesterName: form.get('requesterName'), requesterEmail: form.get('requesterEmail'),
        title: form.get('title'), details: form.get('details'),
      }),
    });
    result.replaceChildren();
    const heading = document.createElement('strong');
    heading.textContent = `Request received · ${friendlyCategory(receipt.category)} · ${receipt.suggestedPriority} priority`;
    const summary = document.createElement('span');
    summary.textContent = `${receipt.recommendedNextAction} Reference: ${receipt.requestId}`;
    result.append(heading, summary);
    result.hidden = false;
    formElement.reset();
    if (workspaceAvailable) await Promise.all([loadRequests(), loadItems(), loadOrganization()]);
  } catch (requestFailure) {
    error.textContent = requestFailure.message;
  } finally {
    submit.disabled = false;
  }
});

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

  document.querySelector('#team-panel').hidden = organization.currentUserRole !== 'ADMIN';
}

async function loadOrganization() {
  if (authenticationConfigured() && !authState.authenticated && !demoMode) return;
  try {
    const organization = await api('/api/saas/organization');
    document.querySelector('#organization-name').textContent = organization.name;
    document.querySelector('#plan-badge').textContent = organization.plan;
    document.querySelector('#usage-summary').textContent =
      `${organization.usage.workItemsUsed}/${organization.usage.workItemsLimit} work items · ` +
      `${organization.usage.agentRunsUsed}/${organization.usage.agentRunsLimit} assisted plans this month`;
    renderUsage(organization);
  } catch (error) {
    document.querySelector('#usage-summary').textContent = error.message;
    document.querySelector('#usage-meters').hidden = true;
    document.querySelector('#saas-meta').hidden = true;
  }
}

function renderMembers(members) {
  const list = document.querySelector('#members-list');
  list.replaceChildren();
  if (!members.length) { list.textContent = 'No members yet.'; return; }
  for (const member of members) {
    const row = document.querySelector('#member-template').content.cloneNode(true);
    row.querySelector('.member-email').textContent = member.email || member.userId;
    row.querySelector('.member-id').textContent = member.joinedAt
      ? `joined ${new Date(member.joinedAt).toLocaleDateString()}` : '';
    const badge = row.querySelector('.role-badge');
    badge.textContent = member.role;
    badge.dataset.role = member.role;
    list.append(row);
  }
}

function invitationStatus(invitation) {
  if (invitation.acceptedAt) return 'accepted';
  return new Date(invitation.expiresAt) < new Date() ? 'expired' : 'pending';
}

function renderInvitations(invitations) {
  const list = document.querySelector('#invitations-list');
  list.replaceChildren();
  if (!invitations.length) { list.textContent = 'No invitations yet.'; return; }
  for (const invitation of invitations) {
    const row = document.querySelector('#invitation-template').content.cloneNode(true);
    row.querySelector('.invitation-email').textContent = invitation.email;
    row.querySelector('.invitation-meta').textContent =
      `${invitation.role} · expires ${new Date(invitation.expiresAt).toLocaleDateString()}`;
    const status = invitationStatus(invitation);
    const badge = row.querySelector('.invitation-status');
    badge.textContent = status;
    badge.dataset.status = status;
    list.append(row);
  }
}

async function loadTeam() {
  if (document.querySelector('#team-panel').hidden) return;
  const teamError = document.querySelector('#team-error');
  teamError.textContent = '';
  try {
    const [members, invitations] = await Promise.all([
      api('/api/saas/members'), api('/api/saas/invitations'),
    ]);
    renderMembers(members);
    renderInvitations(invitations);
  } catch (error) {
    teamError.textContent = error.message;
  }
}

function showInviteToken(invitation) {
  const result = document.querySelector('#invite-result');
  document.querySelector('#invite-message').textContent =
    `Invitation for ${invitation.email} (${invitation.role}) created. Share this one-time token:`;
  document.querySelector('#invite-token').textContent = invitation.token;
  result.hidden = false;
}

document.querySelector('#invite-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const formElement = event.currentTarget;
  const form = new FormData(formElement);
  const submit = formElement.querySelector('button[type="submit"]');
  submit.disabled = true;
  document.querySelector('#team-error').textContent = '';
  try {
    const invitation = await api('/api/saas/invitations', { method: 'POST', body: JSON.stringify({
      email: form.get('email'), role: form.get('role'),
    }) });
    showInviteToken(invitation);
    formElement.reset();
    await loadTeam();
  } catch (error) { document.querySelector('#team-error').textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#copy-token').addEventListener('click', async () => {
  const token = document.querySelector('#invite-token').textContent;
  try {
    await navigator.clipboard.writeText(token);
    document.querySelector('#copy-token').textContent = 'Copied';
    setTimeout(() => { document.querySelector('#copy-token').textContent = 'Copy'; }, 1500);
  } catch { /* clipboard unavailable; token remains visible to copy manually */ }
});

document.querySelector('#refresh-team').addEventListener('click', loadTeam);

document.querySelector('#accept-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const formElement = event.currentTarget;
  const form = new FormData(formElement);
  const submit = formElement.querySelector('button[type="submit"]');
  const result = document.querySelector('#accept-result');
  submit.disabled = true;
  result.textContent = '';
  try {
    const overview = await api('/api/saas/invitations/accept', { method: 'POST', body: JSON.stringify({
      token: form.get('token').trim(),
    }) });
    result.textContent = `Joined ${overview.name} as ${overview.currentUserRole}.`;
    formElement.reset();
    await Promise.all([loadOrganization(), loadTeam()]);
  } catch (error) { result.textContent = error.message; }
  finally { submit.disabled = false; }
});

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

async function loadRequests() {
  requestError.textContent = '';
  try {
    renderRequests(await api('/api/requests'));
  } catch (error) {
    requestError.textContent = error.message;
  }
}

function renderRequests(requests) {
  requestBoard.replaceChildren();
  if (!requests.length) {
    requestBoard.textContent = 'No incoming requests yet. Share the request form to get started.';
    return;
  }
  for (const request of requests) {
    const view = document.querySelector('#request-template').content.cloneNode(true);
    view.querySelector('.request-category').textContent = friendlyCategory(request.category);
    view.querySelector('time').textContent = new Date(request.createdAt).toLocaleString();
    view.querySelector('h3').textContent = request.title;
    view.querySelector('.request-from').textContent = `${request.requesterName} · ${request.requesterEmail}`;
    view.querySelector('.request-summary').textContent = request.internalSummary;
    view.querySelector('.request-action').textContent = `Next: ${request.recommendedNextAction}`;
    const priority = view.querySelector('.priority');
    priority.textContent = request.suggestedPriority;
    priority.dataset.priority = request.suggestedPriority;
    view.querySelector('.request-status').textContent = request.status;
    requestBoard.append(view);
  }
}

function renderRuns(runs) {
  runBoard.replaceChildren();
  if (!runs.length) {
    runBoard.textContent = 'No assisted plans yet.';
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
    view.querySelector('.run-meta').textContent = `${classificationLabel(run.classification)} · up to ${run.toolBudget} steps · ${run.createdWorkItems} work items created`;
    view.querySelector('.run-identity').textContent = `Requested by ${run.userId} · reference ${run.correlationId}`;
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
    board.textContent = 'No work yet. Add a request or create a work plan.';
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
      title: form.get('title'), description: 'Created from the RequestFlow AI dashboard',
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
  result.textContent = 'Preparing a work plan…';
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
      DRY_RUN: 'Preview complete: no work items were created.',
      BUDGET_EXCEEDED: 'Stopped safely: increase the maximum work steps for this plan.',
      BLOCKED: 'Blocked by the safety policy: no work items were created.',
    };
    result.textContent = `${classificationLabel(response.classification)}: ${messages[response.outcome]}`;
    await Promise.all([loadItems(), loadRuns(), loadOrganization()]);
  } catch (error) { result.textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#refresh').addEventListener('click', loadItems);
document.querySelector('#refresh-runs').addEventListener('click', loadRuns);
document.querySelector('#refresh-requests').addEventListener('click', loadRequests);
showBillingReturn();
await loadIntakePortal();
if (workspaceAvailable) {
  await Promise.all([loadItems(), loadRuns(), loadRequests(), loadOrganization()]);
  await loadTeam();
}
