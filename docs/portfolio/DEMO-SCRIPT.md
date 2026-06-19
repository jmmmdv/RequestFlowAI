# RequestFlow AI demo script (5 minutes)

Use this for a first-customer conversation, recorded portfolio demo, or interview. Lead with the
request-management problem, then show the engineering evidence. Keep the scope honest.

## Before you start

- [ ] Run `./mvnw spring-boot:run` or open the labeled demo URL.
- [ ] Open the dashboard, `/swagger-ui.html`, `TenantIsolationSecurityTest`, and a terminal.
- [ ] Optional: start Grafana with `docker compose --profile observability up -d`.
- [ ] Reset browser-local demo data if you want a predictable walkthrough.

## Talk track

**0:00 — The customer problem (30s)**

> "This is RequestFlow AI. Small service teams receive requests through email, messages, calls,
> and forms, then struggle to decide what is urgent and track what happens next. RequestFlow AI is
> being built to collect those requests, turn them into trackable work, and keep the team aligned."

> "What you are seeing is a working SaaS foundation and pilot-ready demo, not a claim that the
> entire commercial product is launched."

**0:30 — Public request journey (60s)**

- Submit “Urgent: customers cannot access the booking form” through the public form.
- Show the category, priority suggestion, reference, and recommended next action.
- Scroll to the request inbox and the automatically created trackable work item.

**1:30 — Rule-based request assistance (75s)**

- Enter a routine request and choose **Create work plan**.
- Show the bounded steps created on the board.
- Enter an urgent request, such as “Urgent production outage for the client portal.”

> "The assistant is intentionally rule-based today. It can apply deterministic classification and
> priority rules, but it is not being marketed as an LLM. High-impact work pauses before side
> effects and waits for a person."

- Approve the pending plan from the automation history.
- Explain that retrying the same idempotency key does not duplicate work.

**2:45 — Tenant safety and audit evidence (60s)**

- Open `TenantIsolationSecurityTest`.

> "Tenant identity comes from a verified JWT boundary, never from a client-submitted tenant ID.
> Tests prove one organization cannot list or fetch another organization's work. Each automated
> decision also records tenant, user, correlation ID, and outcome."

**3:45 — SaaS and delivery foundation (50s)**

- Show `/swagger-ui.html` and mention organizations, invitations, quotas, Checkout, and verified
  webhooks.
- Run or show the result of `./mvnw clean verify`.

> "One command runs the backend, security, contract, PostgreSQL, and coverage gates. Playwright
> verifies the user journeys. The same Flyway migrations are used across local and PostgreSQL
> environments."

**4:35 — Close with the roadmap (25s)**

> "The complete local request-to-work journey is working. The next launch work is production abuse
> protection, privacy and retention controls, a real Start Free identity journey, and recorded
> Cognito, Stripe test, and restore drills. Those gaps remain deliberately visible."

## Useful answers

- **Is this using AI?** The current assistant is rule-based. The repository proves the orchestration,
  safety, evaluation, and audit seam needed before an LLM is added.
- **Can customers submit requests publicly?** Yes. The server resolves the organization from a
  public slug, stores the original request, and creates tenant-owned work. Production rate limiting
  and bot protection are still required before broadly publishing the link.
- **How is tenant leakage prevented?** Verified identity supplies the tenant ID, repository queries
  require it, and automated tests cover list and direct-ID isolation.
- **Are payments live?** Stripe Checkout and verified webhooks are implemented as a foundation.
  Production configuration and real test-mode launch drills are still required.
- **What stops quality regression?** Maven verification, PostgreSQL Testcontainers, Playwright, and
  an enforced 80% line-coverage floor.
- **What would you validate with pilots?** Intake friction, useful request categories, priority
  accuracy, status language, and whether the recommended next action actually saves follow-up time.

## Do not oversell

Do not call this a launched commercial service, claim paying customers, describe the rule-based
assistant as an LLM, or imply the default browser-local demo persists production data. Accurate
scope makes the product and the engineering easier to trust.
