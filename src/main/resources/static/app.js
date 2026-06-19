const board = document.querySelector('#board');
const errorBox = document.querySelector('#error');

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
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
    const response = await api('/api/agent/plan', { method: 'POST', body: JSON.stringify({
      goal: new FormData(event.currentTarget).get('goal'), createWorkItems: true,
    }) });
    result.textContent = `${response.classification}: created ${response.createdWorkItemIds.length} work items.`;
    await loadItems();
  } catch (error) { result.textContent = error.message; }
  finally { submit.disabled = false; }
});

document.querySelector('#refresh').addEventListener('click', loadItems);
loadItems();
