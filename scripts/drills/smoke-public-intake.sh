#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE_URL:-https://e6qeqmdbbd.us-east-1.awsapprunner.com}"
SLUG="${1:-local}"
BASE="https://${API_BASE#https://}"
BASE="${BASE#http://}"

echo "RequestFlow AI — public intake smoke test"
echo "API: https://$BASE"
echo "Slug: $SLUG"
echo

post_intake() {
  local key="$1"
  local body="$2"
  curl -sS -w "\nHTTP:%{http_code}" -X POST "https://$BASE/api/public/intake/$SLUG" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $key" \
    -d "$body"
}

KEY="smoke-$(date +%s)"
RESPONSE="$(post_intake "$KEY" '{"requesterName":"Smoke Test","requesterEmail":"smoke@example.com","companyName":"Smoke Co","title":"Public intake smoke test","details":"Automated validation run."}')"
BODY="$(echo "$RESPONSE" | sed '$d')"
CODE="$(echo "$RESPONSE" | tail -1 | sed 's/HTTP://')"

echo "Happy path:"
echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
echo "Status: $CODE"
echo

BOT_RESPONSE="$(post_intake "bot-$KEY" '{"requesterName":"Bot","requesterEmail":"bot@example.com","companyName":"Bot Co","title":"Spam","details":"Should be rejected by honeypot.","website":"https://spam.example"}')"
BOT_CODE="$(echo "$BOT_RESPONSE" | tail -1 | sed 's/HTTP://')"
echo "Honeypot rejection status: $BOT_CODE"

if [[ "$CODE" == "201" && "$BOT_CODE" == "400" ]]; then
  echo "PASS — intake accepted and honeypot rejected."
  exit 0
fi

echo "FAIL — expected 201 happy path and 400 honeypot." >&2
exit 1
