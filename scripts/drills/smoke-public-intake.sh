#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE_URL:-https://e6qeqmdbbd.us-east-1.awsapprunner.com}"
SLUG="${1:-my-organization}"

echo "RequestFlow AI — public intake smoke test"
echo "API: $API_BASE"
echo "Slug: $SLUG"
echo

KEY="smoke-$(date +%s)"
RESPONSE="$(curl -sS -w "\nHTTP:%{http_code}" -X POST "$API_BASE/api/public/intake/$SLUG" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"requesterName\":\"Smoke Test\",\"requesterEmail\":\"smoke@example.com\",\"companyName\":\"Smoke Co\",\"title\":\"Public intake smoke test\",\"details\":\"Automated validation run.\"}")"

BODY="$(echo "$RESPONSE" | sed '$d')"
CODE="$(echo "$RESPONSE" | tail -1 | sed 's/HTTP://')"

echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
echo "Status: $CODE"

if [[ "$CODE" == "201" ]]; then
  echo "PASS — public intake accepted. Sign in to the dashboard and refresh Requests to confirm."
  exit 0
fi

echo "FAIL — check organization slug in Dashboard → Request portal." >&2
exit 1
