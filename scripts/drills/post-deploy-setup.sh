#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STACK_NAME="${STACK_NAME:-automation-mission-control}"
AWS_REGION="${AWS_REGION:-us-east-1}"
FRONTEND_URL="${FRONTEND_URL:-https://from-zero-to-hero-azure.vercel.app}"
COGNITO_PREFIX="${COGNITO_PREFIX:-jeyhun-requestflow-2026}"
ALARM_EMAIL="${ALARM_EMAIL:-jmmmdv@gmail.com}"

echo "==> Waiting for CloudFormation stack: $STACK_NAME"
for _ in $(seq 1 60); do
  STATUS="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
    --query "Stacks[0].StackStatus" --output text 2>/dev/null || echo MISSING)"
  echo "    $(date +%H:%M:%S) $STATUS"
  case "$STATUS" in
    CREATE_COMPLETE|UPDATE_COMPLETE) break ;;
    *ROLLBACK*|*FAILED*|MISSING)
      echo "Stack failed or missing. Check CloudFormation Events in the AWS Console." >&2
      exit 1 ;;
  esac
  sleep 30
done

IMAGE_URI="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Parameters[?ParameterKey=='ImageRepository'].ParameterValue" --output text)"

echo "==> Fixing FrontendUrl (removes double https:// if present)"
FRONTEND_URL="${FRONTEND_URL#https://}"
FRONTEND_URL="https://${FRONTEND_URL#https://}"

aws cloudformation deploy \
  --stack-name "$STACK_NAME" \
  --template-file "$ROOT/infrastructure/aws/template.yaml" \
  --capabilities CAPABILITY_IAM \
  --region "$AWS_REGION" \
  --parameter-overrides \
    ImageRepository="$IMAGE_URI" \
    FrontendUrl="$FRONTEND_URL" \
    CognitoDomainPrefix="$COGNITO_PREFIX" \
    AlarmEmail="$ALARM_EMAIL"

echo "==> Deploying Cognito trigger (bundles AWS SDK for tenant_id provisioning)"
"$ROOT/scripts/drills/deploy-cognito-trigger.sh"

echo "==> Stack outputs"
aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Outputs" --output table

SERVICE_URL="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='ServiceUrl'].OutputValue" --output text)"
COGNITO_DOMAIN="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='CognitoManagedLoginDomain'].OutputValue" --output text)"
COGNITO_CLIENT_ID="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='CognitoClientId'].OutputValue" --output text)"
USER_POOL_ID="$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$AWS_REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='UserPoolId'].OutputValue" --output text)"

API_BASE="https://${SERVICE_URL}"
ENV_FILE="$ROOT/.requestflow-vercel-env.txt"
cat > "$ENV_FILE" <<EOF
# Paste these into Vercel → Project → Settings → Environment Variables → Production
# Then redeploy the Vercel project.

REQUESTFLOW_API_BASE_URL=$API_BASE
REQUESTFLOW_COGNITO_DOMAIN=$COGNITO_DOMAIN
REQUESTFLOW_COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID
REQUESTFLOW_PUBLIC_ORGANIZATION_SLUG=local
EOF

echo "==> Wrote Vercel env template: $ENV_FILE"
cat "$ENV_FILE"

echo "==> API health check"
curl -fsS "$API_BASE/actuator/health" && echo || echo "Health check not ready yet — App Runner may still be starting."

echo "==> Cognito login domain"
echo "$COGNITO_DOMAIN"
echo "User pool: $USER_POOL_ID"

echo
echo "Done. Next:"
echo "  1. Add env vars from $ENV_FILE to Vercel and redeploy"
echo "  2. Confirm SNS alarm email subscription in your inbox"
echo "  3. Run: ./scripts/drills/verify-prerequisites.sh"
echo "  4. Run: ./scripts/drills/verify-rds-backups.sh"
echo "  5. Start drill 1: docs/saas/EXTERNAL-DRILLS.md"
echo "  6. Stripe (optional): export sk_test/whsec/price IDs → ./scripts/drills/setup-stripe-sandbox.sh"
