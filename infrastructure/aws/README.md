# AWS production deployment

The CloudFormation stack deploys one production-shaped vertical slice:

- App Runner from an immutable ECR image;
- a custom VPC with public NAT and two private application/database subnets;
- encrypted, private PostgreSQL with Multi-AZ, backups, deletion protection, and snapshot retention;
- a generated database password in Secrets Manager, injected at runtime rather than stored in code;
- an App Runner VPC connector and security-group-to-security-group PostgreSQL access;
- CloudWatch golden-signal dashboard, SNS alarm topic, and error/latency/storage alarms;
- optional vendor-neutral OTLP trace export;
- a Cognito user pool, managed login, PKCE browser client, tenant claims, and role groups;
- optional Stripe keys from Secrets Manager for test or live subscription checkout.

This stack creates billable resources, notably RDS, App Runner, and a NAT Gateway. Deploy it in a
sandbox account with a budget alarm, and delete test stacks when the exercise is complete.

## Build and deploy

Prerequisites: AWS CLI credentials, Docker, an ECR repository, and a deployed HTTPS frontend URL.

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

aws cloudformation validate-template \
  --template-body file://infrastructure/aws/template.yaml
aws cloudformation deploy \
  --stack-name automation-mission-control \
  --template-file infrastructure/aws/template.yaml \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    ImageRepository="$IMAGE_URI" \
    FrontendUrl="https://YOUR-PROJECT.vercel.app" \
    CognitoDomainPrefix="YOUR-GLOBALLY-UNIQUE-PREFIX" \
    AlarmEmail="YOUR-EMAIL@example.com"
```

Confirm the SNS email subscription, wait for the App Runner service to become `RUNNING`, then use
the stack outputs for its URL, Cognito client/domain, dashboard, alarm topic, and private database
endpoint. Configure the Vercel project from those outputs:

```bash
vercel env add MISSION_API_BASE_URL production
vercel env add MISSION_COGNITO_DOMAIN production
vercel env add MISSION_COGNITO_CLIENT_ID production
```

These are public browser configuration values, not secrets. Redeploy Vercel after adding them.

## Optional Stripe test billing

Create recurring PRO and BUSINESS prices in Stripe test mode. Store the test secret key and webhook
signing secret as two Secrets Manager secrets, then deploy with `StripeSecretKeyArn`,
`StripeWebhookSecretArn`, `StripeProPriceId`, and `StripeBusinessPriceId`. Point the Stripe webhook
at `https://APP-RUNNER-URL/api/billing/webhook` and subscribe to `checkout.session.completed` and
`customer.subscription.*`. Checkout requests use idempotency keys; webhook bodies are verified
before any subscription or plan state changes.

## Optional OTLP tracing

Set `OtlpTracesEndpoint` to an HTTPS collector endpoint ending in `/v1/traces`. The template enables
tracing only when this parameter is present; normal CloudWatch metrics and alarms always remain on.
The collector should require authentication at its network edge—do not put tokens in the template.

## Production controls

- `DatabaseMultiAz` and `DatabaseDeletionProtection` default to `true`.
- The database has no public IP and accepts port 5432 only from the App Runner connector group.
- CloudFormation replacement or deletion retains a final snapshot.
- Database and optional Stripe secrets are visible only to the App Runner instance role and
  authorized operators.
- App Runner has HTTPS/DNS egress for the OIDC issuer and optional collector; database egress is
  restricted by security-group identity.
- A single NAT Gateway keeps this portfolio stack understandable. A stricter high-availability
  deployment should use one NAT Gateway per Availability Zone and route each private subnet locally.

## Operating evidence

- [Service-level objectives](../../docs/operations/SLO.md)
- [Incident response](../../docs/operations/INCIDENT-RUNBOOK.md)
- [Backup restore drill](../../docs/operations/RESTORE-DRILL.md)

Do not claim a restore is tested until the evidence table in the restore drill contains a real,
timed execution from the target AWS account.
