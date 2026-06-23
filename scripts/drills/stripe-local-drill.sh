#!/usr/bin/env bash
set -euo pipefail

# Forwards Stripe webhooks to a local Spring Boot API. Use with test mode keys only.
# Complete Checkout in the browser while this script runs.

API_PORT="${API_PORT:-8080}"
WEBHOOK_PATH="/api/billing/webhook"

if ! command -v stripe >/dev/null 2>&1; then
  echo "Stripe CLI required. Install: brew install stripe/stripe-cli/stripe" >&2
  exit 1
fi

echo "Forwarding Stripe events to http://localhost:${API_PORT}${WEBHOOK_PATH}"
echo "Set STRIPE_WEBHOOK_SECRET to the whsec_ value printed below when running the API."
echo "Press Ctrl+C to stop."
echo

stripe listen --forward-to "localhost:${API_PORT}${WEBHOOK_PATH}"
