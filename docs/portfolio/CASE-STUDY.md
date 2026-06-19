# Case study: RequestFlow AI

> A production-shaped SaaS MVP foundation that turns incoming business requests into trackable,
> tenant-safe work while preserving approval and audit evidence.

**Role:** sole engineer (product direction, design, build, test, deployment preparation) ·
**Stack:** Java 21, Spring Boot 3.5, PostgreSQL, Playwright, AWS, Stripe ·
**Status:** working foundation and pilot-ready demo; not yet a launched commercial product.

- Live demo: _add your Vercel URL here_
- Source: https://github.com/jmmmdv/FromZeroToHero
- API docs: `/swagger-ui.html` on the running service

---

## The product problem

Small businesses and service teams often receive work through email, WhatsApp, text messages,
calls, and disconnected forms. Requests get lost, urgency is inconsistent, and follow-up becomes
manual. Enterprise service-management products can be too expensive or complicated for teams that
first need one reliable request workspace.

RequestFlow AI is designed to collect requests, suggest what is urgent, turn requests into
trackable work, and give the team a clear status and audit history. The initial market is small
service businesses, agencies, consultants, and small IT/support teams.

## Why this repository exists

The repository has two deliberate jobs:

1. Build a credible SaaS foundation that can grow into a paid request-management product.
2. Provide inspectable evidence of production-shaped Java/Spring Boot engineering.

That dual purpose is why product work sits beside tenant-isolation tests, billing boundaries,
database migrations, browser journeys, infrastructure as code, and operational documentation.

## What is implemented

- A **public request portal** that resolves the destination organization by server-owned slug,
  stores the original request, safely retries through tenant-scoped idempotency, and creates a
  quota-checked work item without accepting a client-submitted tenant ID.
- **Rule-based triage** that suggests SUPPORT/BILLING/SALES/CHANGE/GENERAL category, urgency,
  internal summary, and next action without pretending to use an LLM.
- A **tenant-scoped work-management API** (Spring Web + HATEOAS) with validation, status tracking,
  and RFC 9457-style problem responses.
- A **trusted tenant boundary**: identity comes from a verified JWT claim, repository queries are
  tenant-scoped, and automated tests prove organizations cannot access each other's work.
- A **rule-based planning assistant** that classifies a request-like goal, enforces a safety policy
  and tool budget, suggests urgency through bounded rules, pauses high-impact actions for approval,
  and records tenant, user, correlation ID, and outcome.
- A **SaaS control plane** with organizations, memberships, roles, expiring invitations,
  FREE/PRO/BUSINESS quotas, Stripe Checkout, and signature-verified webhooks.
- A **single delivery gate** covering REST behavior, API/database agreement, tenant security,
  contracts, PostgreSQL migrations, Playwright journeys, and an 80% line-coverage floor.
- A **production path** through Docker, GitHub Actions, Jenkins, CloudFormation, private RDS,
  Cognito configuration, OpenTelemetry, Grafana, CloudWatch, SLOs, and runbooks.

## Key decisions

| Decision | Why it matters |
|---|---|
| Tenant ID comes from a verified JWT claim, never request input | Prevents a common multi-tenant data-leak class; proven by `TenantIsolationSecurityTest` |
| The assistant is deterministic and rule-based today | Makes guardrails and evaluation testable before adding LLM cost and non-determinism |
| High-impact actions require idempotent human approval | Creates a safety boundary without duplicate side effects |
| Stripe webhooks use constant-time HMAC verification and timestamp tolerance | Rejects forged and replayed billing events |
| One Flyway history runs on H2 and PostgreSQL | The schema being tested is the schema intended for deployment |
| JaCoCo fails `verify` below 80% line coverage | Coverage cannot silently erode on `main` |
| Product gaps stay visible in the roadmap | Prevents a strong portfolio from being mistaken for a completed launch |

## Evidence and results

- Tenant isolation is verified for collection and direct-ID access.
- HTTP responses are compared with persisted rows through raw SQL.
- Flyway migrations and API persistence run on PostgreSQL via Testcontainers.
- Assistant safety behavior is gated by a 27-case golden/adversarial evaluation suite.
- Public intake, request inbox, work creation, approval, quota, invitation, status, and CRUD
  journeys are covered by Playwright.
- Build quality is enforced by an 80% JaCoCo line-coverage floor.

These are engineering results, not customer traction metrics. No paying customers or production
payment volume are claimed.

## Honest MVP boundary

The application is a **SaaS MVP foundation**, not the complete RequestFlow AI customer journey yet.

Implemented now:

- responsive landing, pricing, and public request intake;
- original-request storage plus rule-based category, priority, summary, and next action;
- organization-scoped work tracking;
- team roles and invitations;
- rule-based planning, priority keywords, approvals, and audit history;
- plan quotas and Stripe-ready billing boundaries;
- local demo and production-shaped deployment configuration.

Still required for a complete first-pilot flow:

- production rate limiting, bot protection, privacy, and retention controls for public intake;
- production-backed Start Free onboarding;
- real Cognito, invitation-transfer, Stripe test, and restore drills;
- pilot onboarding, privacy, support, and feedback documentation.

The assistant is rule-based, not an LLM. The extension path is documented in
`docs/architecture/AGENTIC-AI.md`. The default public demo uses browser-local disposable data.

See the [demo script](DEMO-SCRIPT.md) for a product-first five-minute walkthrough and the
[job-search kit](JOB-SEARCH-KIT.md) for evidence-backed portfolio copy.
