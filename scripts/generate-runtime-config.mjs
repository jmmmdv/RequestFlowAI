import { writeFile } from 'node:fs/promises';

const config = {
  apiBaseUrl: process.env.REQUESTFLOW_API_BASE_URL ?? process.env.MISSION_API_BASE_URL ?? '',
  authMode: process.env.REQUESTFLOW_AUTH_MODE ?? 'none',
  cognitoDomain: process.env.REQUESTFLOW_COGNITO_DOMAIN ?? process.env.MISSION_COGNITO_DOMAIN ?? '',
  clientId: process.env.REQUESTFLOW_COGNITO_CLIENT_ID ?? process.env.MISSION_COGNITO_CLIENT_ID ?? '',
  publicOrganizationSlug: process.env.REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG ?? 'local',
};

const output = `// Generated at deploy time. These values are public browser configuration, never secrets.\n` +
  `window.REQUESTFLOW_CONFIG = Object.freeze(${JSON.stringify(config, null, 2)});\n` +
  `window.MISSION_CONFIG = window.REQUESTFLOW_CONFIG;\n`;

await writeFile('src/main/resources/static/config.js', output, 'utf8');
console.log(config.authMode !== 'none' || config.clientId
  ? 'Publishing protected RequestFlow AI frontend configuration'
  : 'Publishing credential-free RequestFlow AI pilot demo configuration');
