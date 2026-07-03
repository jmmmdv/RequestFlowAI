# AWS to Neon migration

This guide moves RequestFlow AI from the expensive AWS production-shaped stack to a lean
startup-friendly path. It is a code and operations migration; it does not delete AWS resources for
you.

## What changes

| Old default | New default |
|---|---|
| App Runner API | Existing Spring Boot API on a cheaper Java host |
| RDS PostgreSQL Multi-AZ | Neon Postgres |
| Secrets Manager for app secrets | Hosting provider encrypted environment variables |
| Cognito for MVP auth | Basic Auth from `ADMIN_EMAIL` and `ADMIN_PASSWORD_HASH` |
| VPC + NAT Gateway | No VPC/NAT required for the MVP path |
| CloudFormation production stack | Legacy optional evidence under `infrastructure/aws` |

## Code changes supporting the migration

- `DATABASE_URL` accepts Neon-style `postgresql://...?...sslmode=require` values.
- `prod` defaults to `REQUESTFLOW_SECURITY_PROVIDER=basic`.
- AWS SDK Cognito runtime code is removed from the normal build.
- Vercel runtime config supports `REQUESTFLOW_AUTH_MODE=basic`.
- `.env.example` contains placeholders only.

## Migration steps

1. Run the current verification gate:

   ```bash
   ./mvnw clean verify
   npm ci
   npx playwright install chromium
   npm test
   ```

2. Create a Neon database and copy `DATABASE_URL`.

3. Export API environment variables:

   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   export DATABASE_URL="postgresql://USER:PASSWORD@HOST/neondb?sslmode=require"
   export FRONTEND_URL="https://YOUR-VERCEL-APP.vercel.app"
   export REQUESTFLOW_SECURITY_PROVIDER=basic
   export ADMIN_EMAIL="owner@example.com"
   export ADMIN_PASSWORD_HASH="{bcrypt}YOUR_HASH"
   export ADMIN_ORGANIZATION_NAME="RequestFlow AI"
   ```

4. Start the API and let Flyway migrate Neon:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Configure Vercel browser env:

   ```text
   REQUESTFLOW_API_BASE_URL=https://YOUR-API-HOST
   REQUESTFLOW_AUTH_MODE=basic
   REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG=local
   NEXT_PUBLIC_APP_URL=https://YOUR-VERCEL-APP.vercel.app
   ```

6. Deploy the frontend:

   ```bash
   npm ci
   node scripts/generate-runtime-config.mjs
   vercel deploy
   ```

7. Smoke test:

   - Open the Vercel URL.
   - Submit a public request.
   - Sign in to the dashboard.
   - Move a work item through statuses.
   - Confirm the rows exist in Neon.

## Manual AWS cleanup checklist

Only delete AWS resources after the Neon deployment has been verified and any data you need has
been exported.

- CloudFormation stack
- App Runner service
- RDS database and final snapshot decision
- NAT Gateway
- Elastic IP / public IPv4 allocation
- VPC, subnets, route tables, security groups
- Secrets Manager secrets
- Cognito User Pool and app client if unused
- ECR images/repositories if no longer needed
- CloudWatch dashboards, alarms, and SNS topics

## Risks before deleting AWS permanently

- Existing data in RDS must be exported/imported if you need it.
- Cognito users will not automatically exist in Basic Auth.
- Basic Auth is acceptable for founder-led MVP demos but should be upgraded before broad team use.
- Stripe webhooks must point to the new API host before billing tests.
- SMTP and domain settings may still reference old AWS/App Runner URLs.
- Any external demo links using App Runner or Cognito callback URLs will stop working.

## Legacy AWS location

The AWS files remain in `infrastructure/aws` and `scripts/drills` as optional historical
production-shaped evidence. They are not required for the default Vercel + Neon MVP path.
