#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WORK="$ROOT/scripts/drills/cognito-trigger"
STACK="${STACK_NAME:-automation-mission-control}"
REGION="${AWS_REGION:-us-east-1}"

FUNCTION_NAME="$(aws cloudformation describe-stack-resources --stack-name "$STACK" --region "$REGION" \
  --logical-resource-id CognitoTriggerFunction --query 'StackResources[0].PhysicalResourceId' --output text)"

echo "Packaging Cognito trigger for $FUNCTION_NAME"
rm -rf "$WORK/node_modules" "$WORK/package-lock.json"
(
  cd "$WORK"
  npm init -y >/dev/null 2>&1
  npm install @aws-sdk/client-cognito-identity-provider --silent
  zip -qr /tmp/cognito-trigger.zip index.js node_modules
)

aws lambda update-function-code --function-name "$FUNCTION_NAME" \
  --zip-file fileb:///tmp/cognito-trigger.zip --region "$REGION" >/dev/null

echo "Deployed Cognito trigger Lambda: $FUNCTION_NAME"
