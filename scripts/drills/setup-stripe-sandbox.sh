#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STACK="${STACK_NAME:-automation-mission-control}"
REGION="${AWS_REGION:-us-east-1}"

usage() {
  cat <<'EOF'
Configure Stripe test-mode billing for the App Runner stack.

Prerequisites (Stripe Dashboard → test mode):
  1. Create recurring prices for PRO and BUSINESS plans; copy price_... IDs.
  2. Create webhook endpoint: https://YOUR-APP-RUNNER-URL/api/billing/webhook
     Events: checkout.session.completed, customer.subscription.updated, customer.subscription.deleted
  3. Copy sk_test_... secret key and whsec_... signing secret.

Usage:
  export STRIPE_SECRET_KEY=sk_test_...
  export STRIPE_WEBHOOK_SECRET=whsec_...
  # Optional — created automatically when omitted:
  export STRIPE_PRO_PRICE_ID=price_...
  export STRIPE_BUSINESS_PRICE_ID=price_...
  ./scripts/drills/setup-stripe-sandbox.sh

Optional:
  FRONTEND_URL=https://from-zero-to-hero-azure.vercel.app
  COGNITO_PREFIX=jeyhun-requestflow-2026
  ALARM_EMAIL=you@example.com
EOF
}

FRONTEND_URL="${FRONTEND_URL:-https://from-zero-to-hero-azure.vercel.app}"
COGNITO_PREFIX="${COGNITO_PREFIX:-jeyhun-requestflow-2026}"
ALARM_EMAIL="${ALARM_EMAIL:-j.mmmdv@gmail.com}"

for var in STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET; do
  if [[ -z "${!var:-}" ]]; then
    usage
    echo "Missing $var" >&2
    exit 1
  fi
done

if [[ -z "${STRIPE_PRO_PRICE_ID:-}" || -z "${STRIPE_BUSINESS_PRICE_ID:-}" ]]; then
  if [[ -n "${STRIPE_SECRET_KEY:-}" ]]; then
    echo "==> Creating Stripe test products and prices"
    PRO_PRODUCT="$(curl -sS https://api.stripe.com/v1/products \
      -u "$STRIPE_SECRET_KEY:" \
      -d name="RequestFlow AI Pro" \
      -d description="Pro plan for RequestFlow AI pilot")"
    PRO_PRODUCT_ID="$(echo "$PRO_PRODUCT" | jq -r .id)"
    BUSINESS_PRODUCT="$(curl -sS https://api.stripe.com/v1/products \
      -u "$STRIPE_SECRET_KEY:" \
      -d name="RequestFlow AI Business" \
      -d description="Business plan for RequestFlow AI pilot")"
    BUSINESS_PRODUCT_ID="$(echo "$BUSINESS_PRODUCT" | jq -r .id)"
    STRIPE_PRO_PRICE_ID="$(curl -sS https://api.stripe.com/v1/prices \
      -u "$STRIPE_SECRET_KEY:" \
      -d "product=$PRO_PRODUCT_ID" \
      -d unit_amount=2900 \
      -d currency=usd \
      -d "recurring[interval]=month" | jq -r .id)"
    STRIPE_BUSINESS_PRICE_ID="$(curl -sS https://api.stripe.com/v1/prices \
      -u "$STRIPE_SECRET_KEY:" \
      -d "product=$BUSINESS_PRODUCT_ID" \
      -d unit_amount=7900 \
      -d currency=usd \
      -d "recurring[interval]=month" | jq -r .id)"
    export STRIPE_PRO_PRICE_ID STRIPE_BUSINESS_PRICE_ID
    echo "    PRO price:      $STRIPE_PRO_PRICE_ID"
    echo "    BUSINESS price: $STRIPE_BUSINESS_PRICE_ID"
  fi
fi

SERVICE_URL="$(aws cloudformation describe-stacks --stack-name "$STACK" --region "$REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"
IMAGE_URI="$(aws cloudformation describe-stacks --stack-name "$STACK" --region "$REGION" \
  --query "Stacks[0].Parameters[?ParameterKey=='ImageRepository'].ParameterValue" --output text)"

echo "==> Creating Secrets Manager entries (Stripe test mode only)"
SECRET_KEY_ARN="$(aws secretsmanager create-secret \
  --name "requestflow/stripe/secret-key-$(date +%s)" \
  --secret-string "$STRIPE_SECRET_KEY" \
  --region "$REGION" \
  --query ARN --output text)"
WEBHOOK_ARN="$(aws secretsmanager create-secret \
  --name "requestflow/stripe/webhook-secret-$(date +%s)" \
  --secret-string "$STRIPE_WEBHOOK_SECRET" \
  --region "$REGION" \
  --query ARN --output text)"

echo "    Secret key ARN:  $SECRET_KEY_ARN"
echo "    Webhook ARN:     $WEBHOOK_ARN"
echo
echo "==> Updating CloudFormation stack with Stripe parameters"
aws cloudformation deploy \
  --stack-name "$STACK" \
  --template-file "$ROOT/infrastructure/aws/template.yaml" \
  --capabilities CAPABILITY_IAM \
  --region "$REGION" \
  --parameter-overrides \
    ImageRepository="$IMAGE_URI" \
    FrontendUrl="$FRONTEND_URL" \
    CognitoDomainPrefix="$COGNITO_PREFIX" \
    AlarmEmail="$ALARM_EMAIL" \
    StripeSecretKeyArn="$SECRET_KEY_ARN" \
    StripeWebhookSecretArn="$WEBHOOK_ARN" \
    StripeProPriceId="$STRIPE_PRO_PRICE_ID" \
    StripeBusinessPriceId="$STRIPE_BUSINESS_PRICE_ID"

API_BASE="https://${SERVICE_URL}"
echo
echo "==> Stripe webhook URL (add in Stripe Dashboard if not already):"
echo "    ${API_BASE}/api/billing/webhook"
echo
echo "==> Next: sign in as ADMIN on FREE plan → Upgrade to Pro → test card 4242..."
echo "    Then: ./scripts/drills/record-drill.sh stripe-checkout pass --env \"production $REGION\" ..."
