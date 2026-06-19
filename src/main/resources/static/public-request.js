const config = window.REQUESTFLOW_CONFIG ?? window.MISSION_CONFIG ?? {};
const parameters = new URLSearchParams(location.search);
const organizationSlug = parameters.get('organization') || config.publicOrganizationSlug || 'local';
const portalAccessToken = parameters.get('token') || '';
const apiBaseUrl = (config.apiBaseUrl ?? '').replace(/\/$/, '');
const formPanel = document.querySelector('#request-form-panel');
const form = document.querySelector('#public-request-form');
const confirmation = document.querySelector('#request-confirmation');
const errorBox = document.querySelector('#public-request-error');
const submitButton = form.querySelector('button[type="submit"]');

submitButton.disabled = true;

function friendly(value) {
  return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
}

function intakePath() {
  const path = `/api/public/intake/${encodeURIComponent(organizationSlug)}`;
  return portalAccessToken ? `${path}?token=${encodeURIComponent(portalAccessToken)}` : path;
}

async function request(path, options = {}) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || `Request failed (${response.status})`);
  }
  return response.json();
}

async function loadPortal() {
  if (!/^[a-z0-9-]{3,80}$/.test(organizationSlug)) throw new Error('This request portal is not available.');
  const portal = await request(intakePath());
  document.querySelector('#portal-organization').textContent = portal.organizationName;
  document.title = `Send a request to ${portal.organizationName} — RequestFlow AI`;
  if (portal.portalTokenRequired && !portalAccessToken) {
    document.querySelector('#portal-description').textContent =
      'This request portal requires a private access link from the team.';
    formPanel.hidden = true;
    return;
  }
  submitButton.disabled = false;
}

loadPortal().catch((error) => {
  document.querySelector('#portal-description').textContent = error.message;
  formPanel.hidden = true;
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  const values = new FormData(form);
  submitButton.disabled = true;
  errorBox.textContent = '';
  try {
    const receipt = await request(intakePath(), {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({
        requesterName: values.get('requesterName'),
        requesterEmail: values.get('requesterEmail'),
        companyName: values.get('companyName'),
        title: values.get('title'),
        details: values.get('details'),
        category: values.get('category') || null,
        urgency: values.get('urgency') || null,
        website: values.get('website') || null,
      }),
    });
    document.querySelector('#confirmation-reference').textContent = receipt.referenceNumber;
    document.querySelector('#confirmation-status').textContent = friendly(receipt.status);
    document.querySelector('#confirmation-priority').textContent = friendly(receipt.suggestedPriority);
    document.querySelector('#confirmation-category').textContent = friendly(receipt.category);
    document.querySelector('#confirmation-next').textContent = `What happens next: ${receipt.recommendedNextAction}`;
    formPanel.hidden = true;
    confirmation.hidden = false;
    confirmation.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (error) {
    errorBox.textContent = error.message;
  } finally {
    submitButton.disabled = false;
  }
});

document.querySelector('#send-another-request').addEventListener('click', () => {
  form.reset();
  confirmation.hidden = true;
  formPanel.hidden = false;
  form.querySelector('input').focus();
});
