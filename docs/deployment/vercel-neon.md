# Vercel + Neon deployment

This is the recommended low-cost MVP path for RequestFlow AI. It avoids the legacy AWS stack
resources that are expensive before revenue: NAT Gateway, RDS Multi-AZ, App Runner, VPC, public
IPv4, Secrets Manager, and Cognito.

The current codebase keeps the existing Spring Boot API because that is where the product logic,
tests, Flyway migrations, tenant boundaries, quotas, public intake, and Stripe foundations live.
Vercel serves the static browser app from `src/main/resources/static`. The API can run on any
cheap Java host that supports environment variables and outbound Postgres, using Neon as the
database.

## Architecture

| Layer | Lean MVP choice |
|---|---|
| Frontend | Vercel static deployment from `src/main/resources/static` |
| API | Existing Spring Boot API |
| Database | Neon Postgres using `DATABASE_URL` |
| Auth | Environment-driven Basic Auth for founder-led MVP demos |
| Request analysis | Rule-based fallback unless a paid provider is added later |
| Billing | Stripe test/live env vars only when configured |
| Legacy AWS | Optional archived production-shaped evidence, not the default path |

## Create the Neon database

1. Create a Neon project.
2. Create or select the `neondb` database.
3. Copy the pooled or direct connection string.
4. Keep `sslmode=require` in the URL.

Use this shape:

```bash
DATABASE_URL="postgresql://USER:PASSWORD@HOST/neondb?sslmode=require"
```

Do not commit the real value.

## Local API with Neon

Generate a BCrypt password hash for the admin password. One Docker-based option is:

```bash
docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" "YOUR_ADMIN_PASSWORD" \
  | tr -d ':\n' | sed 's/^/{bcrypt}/'
```

Store the result as `ADMIN_PASSWORD_HASH`, including the `{bcrypt}` prefix. Keep the plain password
only in your password manager.

Run the API:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL="postgresql://USER:PASSWORD@HOST/neondb?sslmode=require"
export FRONTEND_URL="http://localhost:8080"
export REQUESTFLOW_SECURITY_PROVIDER=basic
export ADMIN_EMAIL="owner@example.com"
export ADMIN_PASSWORD_HASH="{bcrypt}REPLACE_WITH_HASH"
export ADMIN_ORGANIZATION_NAME="RequestFlow AI"
./mvnw spring-boot:run
```

Flyway runs automatically and creates the current schema.

## Local frontend

For the existing static frontend served by Spring:

```bash
./mvnw spring-boot:run
```

Open `http://localhost:8080`.

For Vercel static preview config generation:

```bash
REQUESTFLOW_API_BASE_URL="http://localhost:8080" \
REQUESTFLOW_AUTH_MODE="basic" \
REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG="local" \
node scripts/generate-runtime-config.mjs
```

## Vercel environment variables

Set these in Vercel for the browser deployment:

```text
REQUESTFLOW_API_BASE_URL=https://YOUR-API-HOST
REQUESTFLOW_AUTH_MODE=basic
REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG=local
NEXT_PUBLIC_APP_URL=https://YOUR-VERCEL-APP.vercel.app
```

Do not put `DATABASE_URL`, `ADMIN_PASSWORD_HASH`, Stripe secret keys, SMTP password, or
`OPENAI_API_KEY` in the browser project unless that same Vercel project is also hosting trusted
server-side code. Browser-visible env vars are not secrets.

## API host environment variables

Set these wherever the Spring Boot API runs:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://USER:PASSWORD@HOST/neondb?sslmode=require
FRONTEND_URL=https://YOUR-VERCEL-APP.vercel.app
REQUESTFLOW_SECURITY_PROVIDER=basic
ADMIN_EMAIL=owner@example.com
ADMIN_PASSWORD_HASH={bcrypt}YOUR_HASH
ADMIN_ORGANIZATION_NAME=RequestFlow AI
OPENAI_API_KEY=
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
STRIPE_PRO_PRICE_ID=
STRIPE_BUSINESS_PRICE_ID=
```

## Deploy to Vercel

```bash
npm ci
REQUESTFLOW_API_BASE_URL="https://YOUR-API-HOST" \
REQUESTFLOW_AUTH_MODE="basic" \
node scripts/generate-runtime-config.mjs
vercel deploy
```

The existing `vercel.json` publishes the static assets. If you later move the frontend to Next.js,
keep the same API contract and environment variable names.

## Confirm AWS is not used

1. Start the app with no `AWS_*`, `COGNITO_*`, or `REQUESTFLOW_COGNITO_*` variables.
2. Confirm `/actuator/health` returns `UP`.
3. Submit a public request.
4. Sign in to the dashboard with Basic Auth.
5. Confirm records appear in Neon.
6. Confirm application logs do not show AWS SDK, App Runner, RDS, Cognito, or Secrets Manager calls.

## Verification

```bash
./mvnw clean verify
npm ci
npx playwright install chromium
npm test
```
