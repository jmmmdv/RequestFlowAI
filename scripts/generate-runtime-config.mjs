import { writeFile } from 'node:fs/promises';

const config = {
  apiBaseUrl: process.env.MISSION_API_BASE_URL ?? '',
  cognitoDomain: process.env.MISSION_COGNITO_DOMAIN ?? '',
  clientId: process.env.MISSION_COGNITO_CLIENT_ID ?? '',
};

const output = `// Generated at deploy time. These values are public OAuth client configuration, never secrets.\n` +
  `window.MISSION_CONFIG = Object.freeze(${JSON.stringify(config, null, 2)});\n`;

await writeFile('src/main/resources/static/config.js', output, 'utf8');
console.log(config.clientId
  ? 'Publishing authenticated SaaS frontend configuration'
  : 'Publishing credential-free portfolio demo configuration');
