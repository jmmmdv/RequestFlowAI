const board = document.querySelector('#board');
const errorBox = document.querySelector('#error');
const runBoard = document.querySelector('#run-board');
const runError = document.querySelector('#run-error');

async function api(path, options = {}) {
  const { headers = {}, ...requestOptions } = options;
  const response = await fetch(path, {
    ...requestOptions,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null : response.json();
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
    await Promise.all([loadItems(), loadRuns()]);
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
    await loadItems();
  } catch (error) {
    errorBox.textContent = error.message;
    await loadItems();
  } finally {
    control.disabled = false;
  }
}

async function removeItem(id) {
  try {
    await api(`/api/work-items/${id}`, { method: 'DELETE' });
    await loadItems();
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
    await loadItems();
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
    await Promise.all([loadItems(), loadRuns()]);
  } catch (error) { result.textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#refresh').addEventListener('click', loadItems);
document.querySelector('#refresh-runs').addEventListener('click', loadRuns);
Promise.all([loadItems(), loadRuns()]);
