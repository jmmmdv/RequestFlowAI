#!/usr/bin/env bash
# Fallback manual Cognito tenant transfer when automatic sync on invitation accept is unavailable.
set -euo pipefail

: "${USER_POOL_ID:?Set USER_POOL_ID from CloudFormation output UserPoolId}"
: "${FOUNDER_TENANT_ID:?Set FOUNDER_TENANT_ID from founder /api/saas/organization id}"
: "${INVITED_EMAIL:?Set INVITED_EMAIL to the invited user's email}"
INVITED_ROLE="${INVITED_ROLE:-VIEWER}"

echo "Transferring $INVITED_EMAIL to tenant $FOUNDER_TENANT_ID as $INVITED_ROLE"
echo "User pool: $USER_POOL_ID"
read -r -p "Continue? [y/N] " confirm
[[ "$confirm" == [yY] ]] || exit 0

aws cognito-idp admin-update-user-attributes \
  --user-pool-id "$USER_POOL_ID" \
  --username "$INVITED_EMAIL" \
  --user-attributes \
    Name=custom:tenant_id,Value="$FOUNDER_TENANT_ID" \
    Name=custom:organization_name,Value="${ORGANIZATION_NAME:-Shared workspace}"

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

echo
echo "Done. Ask $INVITED_EMAIL to sign in again, then accept the invitation token in the dashboard."
