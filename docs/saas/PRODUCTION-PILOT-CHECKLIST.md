# Production pilot deployment checklist

Use this checklist to move RequestFlow AI from **local GO** to a **hosted founder-led paid pilot**.
This document does not claim production launch is complete — it records what must be true before
you invite a paying pilot customer.

**Related docs**

- [SAAS-LAUNCH.md](SAAS-LAUNCH.md) — product flow and boundaries
- [EXTERNAL-DRILLS.md](EXTERNAL-DRILLS.md) — step-by-step live drills
- [DRILL-LOG.md](DRILL-LOG.md) — append-only evidence (do not mark drills pass without running them)
- [infrastructure/aws/README.md](../../infrastructure/aws/README.md) — build, push, CloudFormation deploy
- [docs/operations/SLO.md](../operations/SLO.md) · [INCIDENT-RUNBOOK.md](../operations/INCIDENT-RUNBOOK.md) · [RESTORE-DRILL.md](../operations/RESTORE-DRILL.md)

---

## Phase 0 — Local build gate (run before every deploy)

Run on the commit you intend to ship:

```bash
./mvnw test
./mvnw clean package
npm ci && npx playwright install chromium && npm test   # with ./mvnw spring-boot:run in another terminal
```

| Check | Pass criteria |
|---|---|
| Unit + integration tests | `./mvnw test` exits 0 |
| Package JAR | `./mvnw clean package` → `BUILD SUCCESS`, artifact in `target/` |
| E2E (optional pre-push) | `npm test` → 14 passed against local app |

---

## Phase 1 — AWS App Runner (backend API)

Reference: [infrastructure/aws/README.md](../../infrastructure/aws/README.md)

### Build and push image

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1
REPOSITORY=automation-mission-control
IMAGE_TAG=$(git rev-parse --short=12 HEAD)
IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$REPOSITORY:$IMAGE_TAG"

aws ecr describe-repositories --repository-names "$REPOSITORY" >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name "$REPOSITORY"
aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
docker build --platform linux/amd64 -t "$IMAGE_URI" .
docker push "$IMAGE_URI"
```

### Deploy / update stack

```bash
aws cloudformation validate-template \
  --template-body file://infrastructure/aws/template.yaml

aws cloudformation deploy \
  --stack-name automation-mission-control \
  --template-file infrastructure/aws/template.yaml \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    ImageRepository="$IMAGE_URI" \
    FrontendUrl="https://request-flow-ai-steel.vercel.app" \
    CognitoDomainPrefix="YOUR-GLOBALLY-UNIQUE-PREFIX" \
    AlarmEmail="YOUR-OPS-EMAIL@example.com"
```

Post-deploy helper (waits for stack, redeploys Cognito trigger):

```bash
FRONTEND_URL="https://request-flow-ai-steel.vercel.app" \
COGNITO_PREFIX="YOUR-GLOBALLY-UNIQUE-PREFIX" \
ALARM_EMAIL="YOUR-OPS-EMAIL@example.com" \
./scripts/drills/post-deploy-setup.sh
```

### App Runner verification

```bash
export API_BASE_URL="https://$(aws cloudformation describe-stacks \
  --stack-name automation-mission-control \
  --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"

curl -sS -o /dev/null -w "%{http_code}\n" "$API_BASE_URL/actuator/health/readiness"
# Expect: 200

curl -sS "$API_BASE_URL/actuator/health" | jq .
# Expect: status UP (mail health disabled unless SMTP_HEALTH_ENABLED=true)
```

| Item | Status | Notes |
|---|---|---|
| App Runner service `RUNNING` | ☐ | AWS Console → App Runner |
| Health check `/actuator/health/readiness` | ☐ | Configured in CloudFormation |
| `SPRING_PROFILES_ACTIVE=prod` | ☑ | Set in template |
| Image tag matches deployed git SHA | ☐ | Record `IMAGE_TAG` in deploy notes |
| `SEED_DATA=false` (no demo seed in prod) | ☑ | Default in `application-prod.properties` |

---

## Phase 2 — PostgreSQL / RDS

Set automatically by CloudFormation (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` secret).

| Item | Status | Notes |
|---|---|---|
| RDS Multi-AZ | ☑ | Template default `DatabaseMultiAz=true` |
| Encryption at rest | ☑ | `StorageEncrypted: true` |
| Private subnet only | ☑ | No public IP |
| Deletion protection | ☑ | Template default |
| Backup retention (7–35 days) | ☐ | Confirm parameter `DatabaseBackupRetentionDays` |
| SNS alarm on low storage | ☑ | `LowDatabaseStorageAlarm` in template |
| Restore drill executed | ☐ **TODO** | [RESTORE-DRILL.md](../operations/RESTORE-DRILL.md) · record in [DRILL-LOG.md](DRILL-LOG.md) drill `aws-restore` |

```bash
./scripts/drills/verify-rds-backups.sh
```

---

## Phase 3 — Cognito (identity)

| Item | Status | Notes |
|---|---|---|
| User pool + managed login deployed | ☐ | Stack output `UserPoolId` |
| Post-confirmation trigger (tenant_id) | ☐ | `./scripts/drills/deploy-cognito-trigger.sh` |
| PKCE browser client | ☑ | Template `UserPoolClient` |
| Groups: ADMIN, MEMBER, VIEWER | ☑ | Template |
| `COGNITO_USER_POOL_ID` on App Runner | ☑ | Template env var |
| `OAUTH2_ISSUER_URI` on App Runner | ☑ | Template env var |
| Signup drill recorded | ☑ | [DRILL-LOG.md](DRILL-LOG.md) `cognito-signup` **pass** 2026-06-19 |
| Invitation transfer drill | ☐ **TODO** | [EXTERNAL-DRILLS.md](EXTERNAL-DRILLS.md) Drill 2 |

### Cognito invitation transfer verification

```bash
export USER_POOL_ID="$(aws cloudformation describe-stacks --stack-name automation-mission-control \
  --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)"

./scripts/drills/cognito-invitation-transfer.sh

./scripts/drills/record-drill.sh cognito-invitation-transfer pass \
  --env "production us-east-1" \
  --operator "YOUR-NAME" \
  --notes "Invited VIEWER shares founder tenant_id after accept + reauth"
```

---

## Phase 4 — Stripe (test mode for paid pilot)

Use **Stripe test mode** until you deliberately switch to live keys for real charges.

| Item | Status | Variable / location |
|---|---|---|
| Stripe test secret key | ☐ **TODO** | `STRIPE_SECRET_KEY` → Secrets Manager → App Runner |
| Webhook signing secret | ☐ **TODO** | `STRIPE_WEBHOOK_SECRET` → Secrets Manager |
| PRO price ID | ☐ **TODO** | `STRIPE_PRO_PRICE_ID` (CloudFormation parameter) |
| BUSINESS price ID | ☐ **TODO** | `STRIPE_BUSINESS_PRICE_ID` |
| Webhook URL registered | ☐ **TODO** | `https://APP-RUNNER-HOST/api/billing/webhook` |
| Webhook events subscribed | ☐ | `checkout.session.completed`, `customer.subscription.*` |
| Checkout drill recorded | ☐ **TODO** | [DRILL-LOG.md](DRILL-LOG.md) drill `stripe-checkout` |

### Wire Stripe into the stack

```bash
export STRIPE_SECRET_KEY=sk_test_...        # from Stripe Dashboard — never commit
export STRIPE_WEBHOOK_SECRET=whsec_...      # from Stripe webhook endpoint
./scripts/drills/setup-stripe-sandbox.sh
```

### Stripe checkout drill

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export ACCESS_TOKEN="eyJ..."   # ADMIN access token after Cognito sign-in

./scripts/drills/stripe-checkout-drill.sh

./scripts/drills/record-drill.sh stripe-checkout pass \
  --env "production us-east-1" \
  --operator "YOUR-NAME" \
  --notes "PRO plan synced after Checkout 4242..."
```

### Billing safe-failure test (without Stripe configured)

```bash
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "$API_BASE_URL/api/billing/checkout" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{"plan":"PRO","idempotencyKey":"billing-safe-fail-01"}'
# Expect: HTTP 503, detail contains "Stripe secret key is not configured"
# (When Stripe IS configured, expect 403 for MEMBER or redirect URL for ADMIN)
```

---

## Phase 5 — SMTP / intake email notifications

Production profile defaults to `INTAKE_NOTIFICATION_EMAIL_MODE=smtp` (`application-prod.properties`).
**CloudFormation does not yet inject SMTP variables** — add them manually after deploy.

| Item | Status | Variable |
|---|---|---|
| Notifications enabled | ☐ | `INTAKE_NOTIFICATION_EMAIL_ENABLED=true` (default) |
| Delivery mode | ☐ | `INTAKE_NOTIFICATION_EMAIL_MODE=smtp` |
| From address | ☐ **TODO** | `INTAKE_NOTIFICATION_FROM` (verified sender in SES/SMTP provider) |
| Fallback recipient (no admin email yet) | ☐ **TODO** | `INTAKE_NOTIFICATION_FALLBACK_RECIPIENT` |
| SMTP host | ☐ **TODO** | `SMTP_HOST` (e.g. `email-smtp.us-east-1.amazonaws.com`) |
| SMTP port | ☐ | `SMTP_PORT` (default `587`) |
| SMTP username | ☐ **TODO** | `SMTP_USERNAME` |
| SMTP password | ☐ **TODO** | `SMTP_PASSWORD` (Secrets Manager recommended) |
| Mail health check | ☐ | `SMTP_HEALTH_ENABLED=true` only after SMTP verified |

**How to apply (until template supports SMTP):** AWS Console → App Runner → your service →
Configuration → Environment variables → add the variables above → Deploy.

**Dev/local behavior:** `INTAKE_NOTIFICATION_EMAIL_MODE=log` logs `INTAKE_NOTIFICATION` lines
instead of sending — safe for local demo.

**Verification:** submit a public intake request; confirm email received OR CloudWatch log line
`INTAKE_NOTIFICATION sent` (not `skipped` unless no admin/fallback recipient).

---

## Phase 6 — Environment variables reference

### App Runner (set by CloudFormation today)

| Variable | Source | Required for hosted pilot |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | template | `prod` |
| `DATABASE_URL` | template | ☑ |
| `DATABASE_USERNAME` | template | ☑ |
| `DATABASE_PASSWORD` | Secrets Manager | ☑ |
| `OAUTH2_ISSUER_URI` | template | ☑ |
| `FRONTEND_URL` | parameter `FrontendUrl` | ☑ CORS |
| `COGNITO_USER_POOL_ID` | template | ☑ invite sync |
| `INTAKE_RATE_LIMIT_ENABLED` | template | `true` |
| `STRIPE_SECRET_KEY` | Secrets Manager | ☐ **TODO** for paid pilot |
| `STRIPE_WEBHOOK_SECRET` | Secrets Manager | ☐ **TODO** for paid pilot |
| `STRIPE_PRO_PRICE_ID` | parameter | ☐ **TODO** |
| `STRIPE_BUSINESS_PRICE_ID` | parameter | ☐ **TODO** |

### App Runner (manual — not in template yet)

| Variable | Required for paid pilot |
|---|---|
| `SMTP_HOST` | ☐ **TODO** |
| `SMTP_USERNAME` | ☐ **TODO** |
| `SMTP_PASSWORD` | ☐ **TODO** |
| `INTAKE_NOTIFICATION_FROM` | ☐ **TODO** |
| `INTAKE_NOTIFICATION_FALLBACK_RECIPIENT` | Recommended until team invites work |

### Vercel (public — not secrets)

Set via `vercel env add` or project settings; generated into `config.js` at build:

| Variable | Required for hosted pilot |
|---|---|
| `REQUESTFLOW_API_BASE_URL` | ☐ **TODO** — App Runner HTTPS URL |
| `REQUESTFLOW_COGNITO_DOMAIN` | ☐ **TODO** — `https://PREFIX.auth.REGION.amazoncognito.com` |
| `REQUESTFLOW_COGNITO_CLIENT_ID` | ☐ **TODO** |
| `REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG` | Optional demo slug |

```bash
node scripts/generate-runtime-config.mjs   # local preview of Vercel build step
```

Legacy aliases `MISSION_*` still work for existing deployments.

---

## Phase 7 — Domain, DNS, TLS

| Layer | Item | Status | Notes |
|---|---|---|---|
| **API** | App Runner default URL | ☐ | `*.awsapprunner.com` — HTTPS included |
| **API** | Custom API domain (optional) | ☐ | Route 53 + App Runner custom domain |
| **Frontend** | Vercel HTTPS | ☐ | `https://request-flow-ai-steel.vercel.app` |
| **Frontend** | `FRONTEND_URL` matches live URL | ☐ | Must match exactly for CORS |
| **Cognito** | Callback URLs include frontend origin | ☐ | `https://request-flow-ai-steel.vercel.app` and trailing-slash variant |
| **Stripe** | Webhook endpoint uses HTTPS App Runner URL | ☐ | |

---

## Phase 8 — CloudWatch logs and alarms

| Item | Status | Notes |
|---|---|---|
| App Runner application logs | ☐ | CloudWatch → Log groups for service |
| RDS PostgreSQL logs export | ☑ | Template `EnableCloudwatchLogsExports` |
| Operations dashboard | ☑ | Stack output `DashboardName` |
| SNS alarm topic | ☑ | Stack output `AlarmTopicArn` |
| Alarm email subscription confirmed | ☐ | Confirm email from SNS after deploy |
| High 5xx alarm | ☑ | `HighErrorRateAlarm` |
| High latency alarm | ☑ | `HighLatencyAlarm` |
| Low RDS storage alarm | ☑ | `LowDatabaseStorageAlarm` |
| Backup failure alarm | ☐ **TODO** | Documented in SLO but not yet in IaC |

```bash
aws cloudwatch describe-alarms --alarm-name-prefix mission-control --region us-east-1
```

---

## Phase 9 — Hosted verification commands

Replace placeholders with your deployed values.

```bash
export API_BASE_URL="https://YOUR-APP-RUNNER-HOST"
export ORG_SLUG="your-pilot-org-slug"
export ACCESS_TOKEN="eyJ..."   # Cognito ADMIN token — obtain after sign-in
```

### Health check

```bash
curl -sS -o /dev/null -w "%{http_code}\n" "$API_BASE_URL/actuator/health/readiness"
```

### Public intake smoke test

```bash
API_BASE_URL="$API_BASE_URL" ./scripts/drills/smoke-public-intake.sh "$ORG_SLUG"
```

Or manually:

```bash
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "$API_BASE_URL/api/public/intake/$ORG_SLUG" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pilot-smoke-$(date +%s)" \
  -d '{"requesterName":"Pilot","requesterEmail":"pilot@example.com","companyName":"Pilot Co",
       "title":"Hosted smoke test","details":"Verifying production intake path end to end."}'
# Expect: HTTP 201, referenceNumber RF-XXXXXXXX
```

### Dashboard / authenticated flows

```bash
curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$API_BASE_URL/api/saas/organization" | jq '.plan, .usage, .onboardingCompleted'

curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$API_BASE_URL/api/requests" | jq 'length'
```

### Pricing UI (browser)

1. Open `https://request-flow-ai-steel.vercel.app` — confirm **$0 / $49 / $99** pricing and FAQ.
2. Sign in → confirm **Upgrade to Pro — $49/mo** visible on FREE plan.
3. Complete founder setup → copy public portal link → submit test request.
4. Confirm request appears in inbox; check email or CloudWatch for `INTAKE_NOTIFICATION`.

---

## Phase 10 — Pilot customer readiness

| Item | Status |
|---|---|
| [PILOT-ONBOARDING.md](../../src/main/resources/static/docs/pilot/ONBOARDING.md) shared | ☐ |
| [PRIVACY.md](../../src/main/resources/static/docs/pilot/PRIVACY.md) linked on public form | ☐ |
| [SUPPORT.md](../../src/main/resources/static/docs/pilot/SUPPORT.md) contact email set | ☐ |
| Session logged in [PILOT-LOG.md](PILOT-LOG.md) | ☐ |
| Founder setup call process documented | ☐ |

---

## GO / NO-GO decision matrix

| Stage | GO when | Current status |
|---|---|---|
| **Local demo** | `./mvnw test` + `npm test` pass; `./mvnw spring-boot:run` works with no secrets | **GO** |
| **Hosted free pilot** | App Runner healthy; Cognito signup drill pass; public intake smoke pass; Vercel `REQUESTFLOW_*` configured; privacy/support docs shared | **CONDITIONAL GO** — signup drill pass; confirm latest image deployed |
| **Hosted paid pilot** | All free-pilot items **plus** Stripe test Checkout drill pass; invitation transfer drill pass; SMTP notifications working; `INTAKE_NOTIFICATION_FROM` set; pilot terms/price ($49 Pro) agreed | **NO-GO** — Stripe, invitation, SMTP, restore drills still **pending** in [DRILL-LOG.md](DRILL-LOG.md) |
| **Public self-serve launch** | Live Stripe; WAF/edge rate limits; restore drill; backup-failure alarms; GDPR operational minimum; 3+ pilot sessions in PILOT-LOG; CI green on `main` | **NO-GO** |

---

## Minimum next actions (hosted paid pilot)

Execute in order:

1. Build and push image; update CloudFormation stack with current `IMAGE_TAG`.
2. Confirm SNS alarm subscription email.
3. Set Vercel `REQUESTFLOW_*` env vars; redeploy frontend.
4. Run `./scripts/drills/setup-stripe-sandbox.sh` and complete **stripe-checkout** drill.
5. Run **cognito-invitation-transfer** drill.
6. Add SMTP + `INTAKE_NOTIFICATION_FROM` + `INTAKE_NOTIFICATION_FALLBACK_RECIPIENT` on App Runner.
7. Run `./scripts/drills/smoke-public-intake.sh YOUR-SLUG` against production API.
8. Record results in [DRILL-LOG.md](DRILL-LOG.md) and first row in [PILOT-LOG.md](PILOT-LOG.md).

---

## Honest claim language

| OK to say | Not OK yet |
|---|---|
| Pilot-ready SaaS foundation | Production launch complete |
| Stripe-ready foundation (test mode) | Live production payments without drill evidence |
| Rule-based triage | AI/LLM chatbot |
| One Cognito signup drill passed | All identity/billing drills passed |
