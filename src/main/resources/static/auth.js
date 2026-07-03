const config = window.REQUESTFLOW_CONFIG ?? window.MISSION_CONFIG ?? {};
const tokenKey = 'mission-control-auth';
const basicTokenKey = 'requestflow-basic-auth';
const verifierKey = 'mission-control-pkce-verifier';
const stateKey = 'mission-control-oauth-state';

function randomUrlSafe(bytes = 32) {
  const data = crypto.getRandomValues(new Uint8Array(bytes));
  return btoa(String.fromCharCode(...data)).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

async function challenge(verifier) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

function redirectUri() {
  const path = location.pathname.replace(/\/$/, '');
  return `${location.origin}${path}`;
}

export function authenticationConfigured() {
  return config.authMode === 'basic' || Boolean(config.cognitoDomain && config.clientId);
}

export function getAccessToken() {
  if (config.authMode === 'basic') return null;
  const session = JSON.parse(sessionStorage.getItem(tokenKey) ?? 'null');
  if (!session || session.expiresAt <= Date.now()) return null;
  return session.accessToken;
}

export function authorizationHeaders() {
  if (config.authMode === 'basic') {
    const credentials = sessionStorage.getItem(basicTokenKey);
    return credentials ? { Authorization: `Basic ${credentials}` } : {};
  }
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function login() {
  if (config.authMode === 'basic') {
    const email = prompt('Admin email');
    if (!email) return;
    const password = prompt('Admin password');
    if (!password) return;
    sessionStorage.setItem(basicTokenKey, btoa(`${email.trim().toLowerCase()}:${password}`));
    location.reload();
    return;
  }
  const verifier = randomUrlSafe(64);
  const state = randomUrlSafe();
  sessionStorage.setItem(verifierKey, verifier);
  sessionStorage.setItem(stateKey, state);
  const query = new URLSearchParams({
    response_type: 'code', client_id: config.clientId, redirect_uri: redirectUri(),
    scope: 'openid email profile', state, code_challenge: await challenge(verifier),
    code_challenge_method: 'S256',
  });
  location.assign(`${config.cognitoDomain}/oauth2/authorize?${query}`);
}

export function logout() {
  sessionStorage.removeItem(tokenKey);
  sessionStorage.removeItem(basicTokenKey);
  if (!authenticationConfigured()) return location.reload();
  if (config.authMode === 'basic') return location.reload();
  const query = new URLSearchParams({ client_id: config.clientId, logout_uri: redirectUri() });
  location.assign(`${config.cognitoDomain}/logout?${query}`);
}

export async function initializeAuth() {
  if (config.authMode === 'basic') {
    return { authenticated: Boolean(sessionStorage.getItem(basicTokenKey)), configured: true, mode: 'basic' };
  }
  if (!authenticationConfigured()) return { authenticated: false, configured: false };
  const query = new URLSearchParams(location.search);
  if (query.has('error')) throw new Error(query.get('error_description') ?? query.get('error'));
  if (query.has('code')) {
    if (query.get('state') !== sessionStorage.getItem(stateKey)) throw new Error('OAuth state verification failed');
    const body = new URLSearchParams({
      grant_type: 'authorization_code', client_id: config.clientId, code: query.get('code'),
      redirect_uri: redirectUri(), code_verifier: sessionStorage.getItem(verifierKey) ?? '',
    });
    const response = await fetch(`${config.cognitoDomain}/oauth2/token`, {
      method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body,
    });
    if (!response.ok) throw new Error('Cognito token exchange failed');
    const tokens = await response.json();
    sessionStorage.setItem(tokenKey, JSON.stringify({
      accessToken: tokens.access_token, idToken: tokens.id_token,
      expiresAt: Date.now() + (tokens.expires_in * 1000) - 30_000,
    }));
    sessionStorage.removeItem(verifierKey); sessionStorage.removeItem(stateKey);
    history.replaceState({}, '', redirectUri());
  }
  return { authenticated: Boolean(getAccessToken()), configured: true };
}

export function apiBaseUrl() { return (config.apiBaseUrl ?? '').replace(/\/$/, ''); }
export function publicOrganizationSlug() { return config.publicOrganizationSlug || 'local'; }
