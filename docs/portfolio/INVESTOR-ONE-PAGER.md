# RequestFlow AI — investor one-pager (honest snapshot)

**Product:** SaaS MVP foundation for small service teams that lose requests across email, messages,
calls, and forms.

**Stage:** Pilot-ready demo with production-shaped engineering — not a launched commercial product.

**Repository:** https://github.com/jmmmdv/RequestFlowAI

**Live demo:** https://from-zero-to-hero-azure.vercel.app

## Problem

Small businesses and agencies need one reliable request workspace before they can justify
enterprise service-management tools. Work arrives everywhere; urgency is inconsistent; follow-up is
manual.

## Solution

RequestFlow AI collects requests through a shareable portal, suggests category/priority/next action
with deterministic rules, turns requests into trackable work, and preserves an audit trail as work
moves forward.

## What is proven in code and tests

- Public intake with server-resolved tenant boundary (no client tenant ID)
- Multi-tenant JWT isolation with security tests
- Organizations, roles, invitations, plan quotas
- Stripe Checkout + signature-verified webhooks (test-ready; live drill pending)
- 80%+ automated test coverage gate, PostgreSQL migrations, Playwright journeys
- AWS deployment path (App Runner, private RDS, Cognito, observability)

## Business model (planned)

| Plan | Target | Limits (today) |
|---|---|---|
| Free | Solo / trial | 25 work items, 10 assisted plans/month |
| Pro | Active service business | 1,000 work items, 500 assisted plans/month |
| Business | Team at scale | 10,000 work items, 5,000 assisted plans/month |

Pricing for pilots is not finalized. Checkout infrastructure exists; production billing drill is
documented but not yet recorded.

## Traction (honest)

- No paying customers yet
- One production Cognito signup drill passed (founder onboarding)
- Seeking first pilot customers for workflow validation

## Why this team can build it

Sole-engineer delivery with inspectable evidence: tenant isolation tests, billing boundaries,
Flyway on H2 + PostgreSQL, CI with browser tests, operational runbooks, and honest documentation
that separates foundation from launch.

## Near-term milestones

1. Complete Stripe test Checkout + invitation transfer drills
2. First 3–5 pilot customers with documented feedback
3. Production abuse controls validated under real traffic
4. Optional LLM triage extension after rule-based safety baseline is stable
