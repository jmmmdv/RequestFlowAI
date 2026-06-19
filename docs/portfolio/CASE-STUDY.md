# Case study: Automation Mission Control

> A production-shaped, multi-tenant SaaS where a team manages a delivery board and a
> bounded planning agent turns goals into attributable, auditable work.

**Role:** sole engineer (design, build, test, deploy) · **Stack:** Java 21, Spring Boot 3.5,
PostgreSQL, Playwright, AWS, Stripe · **Status:** working software with an enforced test gate.

- Live demo: _add your Vercel URL here_
- Source: https://github.com/jmmmdv/FromZeroToHero
- API docs: `/swagger-ui.html` on the running service

---

## The problem

Most portfolio repositories are disconnected tutorials that prove nothing about production
readiness. I wanted one coherent product that shows how the concerns a real SaaS team cares
about — security, multi-tenancy, billing, testing, observability, and safe automation —
actually fit together.

## What I built

- A **multi-tenant REST API** (Spring Web + HATEOAS) for a work-delivery board with validation
  and RFC 9457-style problem responses.
- A **trusted tenant boundary**: identity comes only from a verified JWT claim, enforced at the
  repository layer, with tests proving data cannot cross organizations.
- A **bounded planning agent** that classifies a goal, enforces a safety policy and a tool-call
  budget, pauses high-impact actions for human approval, and writes an audit record (tenant,
  user, correlation ID, outcome) for every run.
- A **SaaS control plane**: organizations, memberships, roles, expiring invitations, FREE/PRO/
  BUSINESS plan quotas, and Stripe Checkout with signature-verified webhooks.
- A **single delivery gate**: REST, database-consistency, security, contract, and Playwright
  browser tests, plus an 80% coverage floor that fails the build when missed.
- A **path to production**: Flyway migrations that run identically on H2, CI PostgreSQL, and
  RDS; Docker; GitHub Actions and Jenkins; CloudFormation; and observability with
  OpenTelemetry, Grafana, and CloudWatch.

## Key engineering decisions (the interview material)

| Decision | Why it matters |
|---|---|
| Tenant ID comes from a verified JWT claim, never from request input | Prevents the most common multi-tenant data-leak class; proven by `TenantIsolationSecurityTest` |
| The agent is deterministic and rule-based (for now) | Makes orchestration, guardrails, and evaluation testable before adding an LLM's cost and non-determinism |
| High-impact agent actions require human approval, and approval is idempotent | Safety boundary plus safe-to-retry semantics; no duplicate side effects |
| Stripe webhooks are verified with constant-time HMAC and a timestamp tolerance | Rejects forged and replayed billing events |
| One Flyway migration history across H2, CI, and PostgreSQL | The schema you test is the schema you ship |
| 80% coverage gate enforced in `verify` | Coverage cannot silently erode on `main` |

## Results / evidence

- **Tenant isolation** verified by automated security tests (list and direct-ID access).
- **API/DB agreement** checked by comparing HTTP JSON against raw SQL.
- **Migrations on real PostgreSQL** via Testcontainers.
- **Agent safety** gated by a 27-case golden/adversarial evaluation suite in CI.
- **End-to-end journeys** (dashboard, agent approval, quota meters, team invitations, REST CRUD)
  covered by Playwright.
- **Build quality** enforced by JaCoCo at 80% line coverage.

## Skills this demonstrates

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security, OAuth2 resource
  server, HATEOAS, Flyway.
- Quality: JUnit, integration tests, Testcontainers, Playwright, contract tests, coverage gating.
- Product/SaaS: multi-tenancy, RBAC, invitations, plan quotas, Stripe billing.
- DevOps/Cloud: Docker, GitHub Actions, Jenkins, AWS App Runner/RDS, CloudFormation, observability.
- Automation/AI: bounded agent design, guardrails, idempotency, audit, evaluation.

## Honest scope

- The planning agent is intentionally **rule-based**, not an LLM. The extension path is documented
  in `docs/architecture/AGENTIC-AI.md`.
- The default public deployment is a **browser-local portfolio preview** with disposable data; the
  same build becomes the production UI when the documented environment variables are set
  (see `docs/saas/SAAS-LAUNCH.md`).
- A few operational drills (restore drill, live Cognito/Stripe identity-transfer) are labelled as
  pending in the README roadmap rather than claimed as done.

See the [demo script](DEMO-SCRIPT.md) for a rehearsed 5-minute walkthrough and likely interview
questions, and the [job-search kit](JOB-SEARCH-KIT.md) for ready-to-use LinkedIn, resume, and
outreach copy.
