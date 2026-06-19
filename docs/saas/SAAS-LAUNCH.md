# SaaS launch guide

This package turns the portfolio application into a deployable SaaS foundation without making a
false claim that paid cloud resources were provisioned. Local development remains frictionless;
the production profile requires signed Cognito JWTs and tenant claims.

## Product flow

1. A user signs up in Cognito managed login. The pre-signup trigger assigns an immutable tenant UUID.
2. Post-confirmation adds the founder to `ADMIN`; the token trigger emits tenant and organization claims.
3. The Vercel browser client uses Authorization Code + PKCE and keeps tokens in `sessionStorage`.
4. The API lazily creates the organization, founder membership, and FREE subscription record.
5. Every repository operation uses the verified tenant claim; quota checks run before writes/tools.
6. An admin upgrades through Stripe Checkout. Only a verified webhook changes the effective plan.

## Plans and enforcement

| Plan | Work items | Agent runs per calendar month |
|---|---:|---:|
| FREE | 25 | 10 |
| PRO | 1,000 | 500 |
| BUSINESS | 10,000 | 5,000 |

Quota failures return HTTP `429` Problem Details. Agent idempotency is checked before consuming a
quota slot, and an approved plan reserves capacity for all work items before tools execute.

## Browser deployment

`scripts/generate-runtime-config.mjs` writes only public values into `config.js` during the Vercel
build. Configure these production variables:

```text
MISSION_API_BASE_URL=https://YOUR-APP-RUNNER-HOST
MISSION_COGNITO_DOMAIN=https://YOUR-PREFIX.auth.YOUR-REGION.amazoncognito.com
MISSION_COGNITO_CLIENT_ID=YOUR-PUBLIC-CLIENT-ID
```

With all identity values present, the UI displays sign-in/sign-out and calls the remote API with an
access token. Without them, a `.vercel.app` deployment intentionally becomes the labeled local-data
portfolio demo. Add `?demo` to force demo mode while diagnosing production identity.

## Invitation boundary

Invitations are random 256-bit, hashed at rest, email-bound, expire after seven days, and are
single-use. Acceptance records membership. Cognito remains the authority for tenant claims and
groups, so a real invitation drill must also transfer the invited account's custom tenant attribute
and group through trusted identity administration, then force reauthentication. That final external
identity operation is deliberately not faked by this repository and is tracked as the remaining
launch drill in the README.

## Stripe boundary

The browser never receives a Stripe secret. The API creates Checkout Sessions using an idempotency
key and tenant metadata. The webhook controller verifies the timestamped HMAC over the unmodified
request body with a five-minute tolerance and constant-time comparison. Unknown subscription events
are harmless; inactive/cancelled subscriptions fall back to FREE.

## Evidence to show an interviewer

```bash
./mvnw -Dtest=SaasProductIntegrationTest,TenantIsolationSecurityTest test
./mvnw clean verify
npm test
```

Then walk through the organization overview, a rejected FREE-plan quota request, admin-only invite,
Stripe test Checkout, a signed webhook, and the resulting plan/usage change. Record the external
AWS and Stripe drill date only after it has actually run.
