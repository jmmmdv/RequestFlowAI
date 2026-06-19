#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STACK_NAME="${STACK_NAME:-automation-mission-control}"
AWS_REGION="${AWS_REGION:-us-east-1}"

pass() { printf '  ✓ %s\n' "$1"; }
warn() { printf '  ! %s\n' "$1"; }
fail() { printf '  ✗ %s\n' "$1"; MISSING=1; }

MISSING=0

echo "RequestFlow AI — external drill prerequisites"
echo

echo "Local regression gate (required before recording drills):"
if [[ -x "$ROOT/mvnw" ]]; then
  pass "Maven wrapper present"
else
  fail "Maven wrapper missing"
fi

echo
echo "CLI tools:"
if command -v aws >/dev/null 2>&1; then
  pass "AWS CLI installed ($(aws --version 2>&1 | head -1))"
  if aws sts get-caller-identity --output text >/dev/null 2>&1; then
    pass "AWS credentials active ($(aws sts get-caller-identity --query Account --output text))"
  else
    warn "AWS CLI installed but credentials not configured (run: aws configure)"
  fi
else
  fail "AWS CLI not installed (brew install awscli)"
fi

if command -v stripe >/dev/null 2>&1; then
  pass "Stripe CLI installed ($(stripe --version 2>&1 | head -1))"
else
  warn "Stripe CLI not installed (optional for App Runner drill; required for stripe-local-drill.sh)"
fi

if command -v jq >/dev/null 2>&1; then
  pass "jq installed"
else
  warn "jq not installed (recommended for API evidence curls)"
fi

echo
echo "CloudFormation stack (optional — skip if not deployed yet):"
if command -v aws >/dev/null 2>&1 && aws sts get-caller-identity >/dev/null 2>&1; then
  if aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
    pass "Stack '$STACK_NAME' exists in $AWS_REGION"
    API_URL="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
      --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text 2>/dev/null || true)"
    if [[ -n "$API_URL" && "$API_URL" != "None" ]]; then
      pass "App Runner URL: $API_URL"
      if curl -fsS "$API_URL/actuator/health" >/dev/null 2>&1; then
        pass "API health endpoint reachable"
      else
        warn "API health check failed — confirm App Runner is RUNNING"
      fi
    else
      warn "ServiceUrl output not found"
    fi
  else
    warn "Stack '$STACK_NAME' not found in $AWS_REGION — deploy first (see infrastructure/aws/README.md)"
  fi
else
  warn "Skipping stack checks (AWS unavailable)"
fi

echo
echo "Documentation:"
pass "Runbook: docs/saas/EXTERNAL-DRILLS.md"
pass "Evidence log: docs/saas/DRILL-LOG.md"

echo
if [[ "$MISSING" -eq 0 ]]; then
  echo "Ready to start external drills (deploy stack if not already done)."
  exit 0
fi
echo "Install missing required tools, then re-run this script."
exit 1
