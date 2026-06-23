# External launch drills

These drills require a real sandbox AWS stack, Cognito user pool, and Stripe **test mode**.
Integration tests prove the code paths; this runbook proves your **deployed** environment works end
to end.

Do not mark a drill complete in [DRILL-LOG.md](DRILL-LOG.md) until you have run it against live
infrastructure and captured evidence.

## Before you start

1. Deploy the CloudFormation stack — see [infrastructure/aws/README.md](../../infrastructure/aws/README.md).
2. Configure Vercel (or your HTTPS frontend) with `REQUESTFLOW_*` values from stack outputs.
3. Install tools:

```bash
brew install awscli stripe/stripe-cli/stripe   # macOS example
aws configure
stripe login
```

4. Verify readiness:

```bash
./scripts/drills/verify-prerequisites.sh
```

5. Export common variables (replace placeholders from stack outputs):

```bash
export AWS_REGION=us-east-1
export STACK_NAME=automation-mission-control
export API_BASE_URL="https://$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"
export USER_POOL_ID="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)"
export COGNITO_DOMAIN="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='CognitoManagedLoginDomain'].OutputValue" --output text)"
export COGNITO_CLIENT_ID="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
  --query "Stacks[0].Outputs[?OutputKey=='CognitoClientId'].OutputValue" --output text)"
```

---

## Drill 1 — Cognito signup and Start Free onboarding

**Goal:** A new founder signs up, receives tenant claims, completes workspace setup, and lands in
the dashboard on the FREE plan.

### Steps

1. Open your production frontend URL in a private browser window.
2. Click **Start free** → **Sign in** → create a new account with a fresh email.
3. Confirm email if Cognito requires verification.
4. Complete the **Set up your workspace** wizard (organization name + slug).
5. In the dashboard, confirm:
   - Plan badge shows `FREE`
   - Usage meters show `25` work items / `10` assisted plans
   - **Request portal** panel shows your shareable form link with the slug you chose

### API evidence (optional)

Decode the access token at [jwt.io](https://jwt.io) or:

```bash
# After signing in, paste the bearer token from browser sessionStorage
export ACCESS_TOKEN="eyJ..."
python3 - <<'PY'
import os, json, base64
token = os.environ["ACCESS_TOKEN"].split(".")[1]
print(json.dumps(json.loads(base64.urlsafe_b64decode(token + "==")), indent=2))
PY
```

Confirm claims include `tenant_id`, `organization_name`, and `cognito:groups` contains `ADMIN`.

```bash
curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" "$API_BASE_URL/api/saas/organization" | jq .
```

Expect `onboardingCompleted: true`, `plan: "FREE"`, and `currentUserRole: "ADMIN"`.

### Record

```bash
./scripts/drills/record-drill.sh cognito-signup pass \
  --env "sandbox $AWS_REGION" \
  --notes "Founder onboarded; JWT tenant_id matches /api/saas/organization id"
```

---

## Drill 2 — Team invitation and Cognito tenant transfer

**Goal:** An admin invites a teammate; the invited user joins the **same** tenant after Cognito
attributes and groups are updated (external identity step).

### Part A — Create invitation (in product)

1. Sign in as founder **ADMIN**.
2. Open **Team & access** → invite `teammate+drill@example.com` as `VIEWER`.
3. Copy the one-time invitation token immediately.

### Part B — Invited user signs up (separate browser)

1. Sign up `teammate+drill@example.com` in Cognito (new account gets its **own** tenant by default).
2. Note the founder's `tenant_id` from drill 1 (organization UUID from `/api/saas/organization`).

### Part C — Transfer identity (AWS CLI)

Replace values:

```bash
export FOUNDER_TENANT_ID="00000000-0000-0000-0000-000000000001"   # from founder JWT/API
export INVITED_EMAIL="teammate+drill@example.com"
export INVITED_ROLE="VIEWER"   # must match invitation role

aws cognito-idp admin-update-user-attributes \
  --user-pool-id "$USER_POOL_ID" \
  --username "$INVITED_EMAIL" \
  --user-attributes \
    Name=custom:tenant_id,Value="$FOUNDER_TENANT_ID" \
    Name=custom:organization_name,Value="Founder Workspace Name"

aws cognito-idp admin-remove-user-from-group \
  --user-pool-id "$USER_POOL_ID" \
  --username "$INVITED_EMAIL" \
  --group-name ADMIN 2>/dev/null || true

aws cognito-idp admin-add-user-to-group \
  --user-pool-id "$USER_POOL_ID" \
  --username "$INVITED_EMAIL" \
  --group-name "$INVITED_ROLE"

aws cognito-idp admin-user-global-sign-out \
  --user-pool-id "$USER_POOL_ID" \
  --username "$INVITED_EMAIL"
```

Or run the helper (same commands, with prompts):

```bash
./scripts/drills/cognito-invitation-transfer.sh
```

### Part D — Accept invitation (in product)

1. Invited user signs in again (fresh tokens).
2. Paste invitation token → **Accept invitation**.
3. Confirm dashboard shows founder organization name and `VIEWER` role.

```bash
curl -sS -H "Authorization: Bearer $INVITED_ACCESS_TOKEN" \
  "$API_BASE_URL/api/saas/organization" | jq '.name, .currentUserRole, .id'
```

`id` must equal `$FOUNDER_TENANT_ID`.

### Record

```bash
./scripts/drills/record-drill.sh cognito-invitation-transfer pass \
  --env "sandbox $AWS_REGION" \
  --notes "Invited VIEWER shares founder tenant_id after admin attribute transfer"
```

---

## Drill 3 — Stripe test Checkout and webhook

**Goal:** Admin upgrades through real Stripe test Checkout; verified webhook updates plan to PRO.

### Prerequisites

- Stack deployed with `StripeSecretKeyArn`, `StripeWebhookSecretArn`, `StripeProPriceId`,
  `StripeBusinessPriceId` (see [infrastructure/aws/README.md](../../infrastructure/aws/README.md)).
- Stripe Dashboard webhook endpoint: `$API_BASE_URL/api/billing/webhook`
- Events: `checkout.session.completed`, `customer.subscription.updated`,
  `customer.subscription.deleted`

### Steps

1. Sign in as **ADMIN** on FREE plan.
2. Click **Upgrade to Pro** → complete Checkout with test card `4242 4242 4242 4242`.
3. Return to dashboard; wait for webhook processing (usually seconds).
4. Confirm plan badge `PRO` and limits `1000` / `500`.

```bash
curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" "$API_BASE_URL/api/saas/organization" | jq '.plan, .usage'
```

### Optional — Stripe CLI forwarding (local API debugging)

When testing against `localhost` instead of App Runner:

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...   # from `stripe listen` output
export STRIPE_PRO_PRICE_ID=price_...
./mvnw spring-boot:run

# separate terminal
./scripts/drills/stripe-local-drill.sh
```

### Cancellation check

1. Cancel the test subscription in Stripe Dashboard.
2. Confirm workspace returns to FREE limits after `customer.subscription.deleted` webhook.

### Record

```bash
./scripts/drills/record-drill.sh stripe-checkout pass \
  --env "stripe test mode" \
  --notes "Checkout session completed; plan PRO; cancel restored FREE"
```

---

## Drill 4 — AWS PostgreSQL restore

**Goal:** Prove you can restore from RDS backup within documented RPO/RTO targets.

Follow [RESTORE-DRILL.md](../operations/RESTORE-DRILL.md) exactly. Wrapper:

```bash
./scripts/drills/restore-drill.sh
```

After completion, append the evidence table row in `RESTORE-DRILL.md` and record:

```bash
./scripts/drills/record-drill.sh aws-restore pass \
  --env "sandbox $AWS_REGION" \
  --notes "RPO Xm RTO Ym; flyway history + row counts validated"
```

---

## After all drills

1. Review [DRILL-LOG.md](DRILL-LOG.md) — every required drill should show `pass` with a date.
2. Update [LAUNCH-CHECKLIST.md](../../src/main/resources/static/docs/pilot/LAUNCH-CHECKLIST.md)
   checkboxes that now have evidence.
3. Run regression gate:

```bash
./mvnw clean verify
npm test
```

Only after real drill dates are recorded should README launch items move from open to complete.
