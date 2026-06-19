# Demo script (5 minutes)

Use this for a recorded video or a live interview. Goal: prove the project is real, safe, and
production-shaped — without rambling. Practice it twice; aim for 5 minutes.

## Before you start

- [ ] App running: `./mvnw spring-boot:run` (or your live demo URL open).
- [ ] Tabs open: dashboard, `/swagger-ui.html`, the `TenantIsolationSecurityTest` file, and a
      terminal in the repo.
- [ ] Optional: observability lab up (`docker compose --profile observability up -d`) with Grafana.
- [ ] Say the one-line pitch out loud once so it is crisp.

## Talk track

**0:00 — Framing (20s)**
> "This is Automation Mission Control: a multi-tenant SaaS where a team manages a delivery board
> and a bounded agent turns goals into auditable work. I built it to show production concerns —
> security, billing, testing, and safe automation — working together, not as separate demos."

**0:20 — Product tour (60s)**
- Show the dashboard: work items, the SaaS panel (plan + quota meters), and the Team & access panel.
- Create a work item. Point out it is tenant-scoped and validated.

**1:20 — The agent and its guardrails (75s)**
- Enter a routine goal → show it executes and creates work items.
- Enter an urgent/high-impact goal (e.g. "Fix urgent production outage").
  > "Notice zero tools ran. The policy classified this as high-impact and paused it for approval."
- Approve it from the audit trail.
  > "Approval is idempotent — retrying never creates duplicate work. Every run records tenant,
  > user, correlation ID, and outcome."

**2:35 — Security is the headline (60s)**
- Open `TenantIsolationSecurityTest`.
  > "Tenant identity comes only from a verified JWT claim, never from request input. This test
  > proves one organization cannot list or directly fetch another's data. That's the number-one
  > multi-tenant failure mode, and it's covered."

**3:35 — Proof it's production-shaped (60s)**
- Show `/swagger-ui.html` (discoverable API) and mention Stripe Checkout + signed webhooks.
- In the terminal: `./mvnw clean verify`.
  > "One command runs unit, integration, security, contract, and PostgreSQL Testcontainers tests,
  > and fails below 80% coverage. The same Flyway migrations run on H2, CI, and RDS."

**4:35 — Close (25s)**
> "So: a real SaaS control plane, a safe auditable agent, and a single enforced delivery gate, with
> a documented path to AWS. Happy to go deep on any layer."

## Likely interview questions and crisp answers

- **"How do you stop tenant data leaking?"** Tenant ID is read from a verified JWT claim and applied
  at the repository layer; client input is never trusted. `TenantIsolationSecurityTest` proves list
  and direct-ID isolation.
- **"Why a rule-based agent and not an LLM?"** I wanted orchestration, guardrails, budgets, and
  evaluation to be deterministic and testable first. The LLM seam is documented; the safety scaffold
  is the hard and valuable part.
- **"How are billing webhooks secured?"** Constant-time HMAC signature verification plus a timestamp
  tolerance, so forged or replayed Stripe events are rejected.
- **"What stops a coverage regression?"** JaCoCo fails `verify` below 80% line coverage, enforced in CI.
- **"How do you keep dev and prod schemas in sync?"** One Flyway migration history runs on H2, CI
  PostgreSQL, and RDS — the schema I test is the schema I ship.
- **"What would you do next?"** Introduce the LLM behind the existing policy/budget seam, run the
  restore drill in an AWS sandbox, and add a customer billing portal.

## Don't oversell

If asked what is not done, be honest: the agent is rule-based, the default public site is a
browser-local preview, and a couple of operational drills are still pending (and labelled as such
in the README). Honesty about scope reads as senior.
