#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STACK="${STACK_NAME:-automation-mission-control}"
REGION="${AWS_REGION:-us-east-1}"

usage() {
  cat <<'EOF'
Guide the Stripe test Checkout drill against the deployed App Runner stack.

Prerequisites:
  1. Stripe test-mode secret key and webhook signing secret
  2. Stack updated via ./scripts/drills/setup-stripe-sandbox.sh

Usage:
  export STRIPE_SECRET_KEY=sk_test_...
  export STRIPE_WEBHOOK_SECRET=whsec_...
  ./scripts/drills/stripe-checkout-drill.sh

After completing Checkout in the browser, record evidence:
  ./scripts/drills/record-drill.sh stripe-checkout pass \
    --env "production REGION" \
    --notes "PRO plan synced after Checkout 4242..."
EOF
}

for var in STRIPE_SECRET_KEY STRIPE_WEBHOOK_SECRET; do
  if [[ -z "${!var:-}" ]]; then
    usage
    echo "Missing $var" >&2
    exit 1
  fi
done

SERVICE_URL="$(aws cloudformation describe-stacks --stack-name "$STACK" --region "$REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"
API_BASE="https://${SERVICE_URL}"
FRONTEND_URL="${FRONTEND_URL:-https://from-zero-to-hero-azure.vercel.app}"

echo "==> RequestFlow AI Stripe checkout drill"
echo "    API:      $API_BASE"
echo "    Frontend: $FRONTEND_URL"
echo "    Webhook:  $API_BASE/api/billing/webhook"
echo
echo "Steps:"
echo "  1. Run ./scripts/drills/setup-stripe-sandbox.sh if Stripe is not wired into the stack yet."
echo "  2. Sign in as ADMIN on the FREE plan at $FRONTEND_URL"
echo "  3. Click Upgrade to Pro and complete Checkout with card 4242 4242 4242 4242"
echo "  4. Confirm plan badge PRO and usage limits 1000 / 500"
echo
read -r -p "Press Enter when Checkout is complete (or Ctrl+C to abort)..."

echo "==> Checking organization plan (requires ACCESS_TOKEN from signed-in ADMIN)"
if [[ -z "${ACCESS_TOKEN:-}" ]]; then
  echo "Set ACCESS_TOKEN to the Cognito access token, then re-run:" >&2
  echo "  export ACCESS_TOKEN='eyJ...'" >&2
  echo "  curl -sS -H \"Authorization: Bearer \$ACCESS_TOKEN\" \"$API_BASE/api/saas/organization\" | jq '.plan, .usage'" >&2
  exit 1
fi

curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" "$API_BASE/api/saas/organization" | jq '.plan, .usage, .subscriptionStatus'
echo
echo "Record when plan is PRO:"
echo "  ./scripts/drills/record-drill.sh stripe-checkout pass --env \"production $REGION\" --notes \"Checkout + webhook synced PRO plan\""
