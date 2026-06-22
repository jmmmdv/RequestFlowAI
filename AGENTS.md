# AGENTS.md

## Project identity

This repository is published as **[RequestFlowAI](https://github.com/jmmmdv/RequestFlowAI)** on GitHub.
The product being developed inside it is **RequestFlow AI**.

RequestFlow AI is a SaaS MVP foundation for small businesses, agencies, consultants, and small
IT/support teams. It helps them collect client or internal requests, classify and prioritize those
requests, turn them into trackable work items, manage status, preserve audit evidence, and later
upgrade to paid plans.

The previous product identity was **Automation Mission Control**. When changing user-facing
product copy, documentation, UI labels, and demo material, gradually reposition the product toward
**RequestFlow AI**. Preserve useful engineering evidence from the old version unless explicitly
asked to remove it.

This repository must serve two purposes:

1. A real SaaS MVP foundation that can be shown to first pilot users.
2. A strong professional Java/Spring Boot backend portfolio proving production-shaped engineering
   skills.

Do not treat this as a random tutorial repository. Every change should make the project more
useful, safer, clearer, more testable, and closer to a real paid SaaS product.

## Product direction

RequestFlow AI should solve this problem:

Small businesses receive client or internal requests from email, WhatsApp, text messages, calls,
and forms. Requests get lost, priorities are unclear, follow-up is messy, and owners do not have
one simple dashboard to track the work.

RequestFlow AI should provide:

- a customer-facing landing page
- a simple Start Free flow
- a public request intake form
- request classification
- priority suggestion
- internal summary
- recommended next action
- work-item/status tracking
- organization/team support
- tenant-safe data boundaries
- audit trail for important actions
- Free/Pro/Business plan limits
- Stripe billing foundation
- demo mode for first customer walkthroughs
- production mode documentation

Keep the language business-friendly. Customer-facing pages should avoid unnecessary developer
jargon.

## Target users

Prioritize these users:

- small business owners
- small service businesses
- web/design/marketing agencies
- consultants
- small IT/support teams
- teams that need a simple request portal before they are ready for expensive enterprise tools

Do not build for “everyone.” Keep the MVP focused on request intake, organization, tracking, and
simple automation assistance.

## Main stack

Backend:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth2 Resource Server
- HATEOAS
- Flyway
- H2 for local/test
- PostgreSQL for production-shaped integration
- Testcontainers
- JaCoCo

Frontend / automation:

- JavaScript
- Static dashboard under Spring resources
- Playwright E2E tests

Delivery / operations:

- Maven
- Docker
- Docker Compose
- Jenkins
- GitHub Actions
- AWS CloudFormation / App Runner / RDS concepts
- Observability with Prometheus, Grafana, Tempo, OpenTelemetry, and CloudWatch concepts

SaaS / product:

- organizations
- memberships
- roles
- invitations
- tenant isolation
- plan quotas
- Stripe Checkout
- verified Stripe webhooks
- audit trail
- demo mode vs production mode separation

## Non-negotiable engineering rules

Before making changes:

1. Read `README.md`.
2. Read this `AGENTS.md`.
3. Inspect the relevant source and test files.
4. Prefer small, safe, reviewable changes.
5. Do not rewrite the architecture unless explicitly asked.
6. Do not delete existing tests to make the build pass.
7. Do not weaken security, tenant isolation, auditability, idempotency, or coverage gates.
8. Do not trust client-submitted tenant IDs as an authority.
9. Tenant identity must come from a safe server-side or verified identity boundary.
10. Do not hardcode secrets.
11. Do not commit API keys, Stripe secrets, database passwords, Cognito secrets, tokens, or private
    credentials.
12. Do not expose Stripe secret keys or other server secrets to browser code.
13. Do not claim a feature is complete unless the code, tests, and documentation prove it.
14. If a feature is demo-only, local-only, a lab, a roadmap item, or a planned extension, label it
    clearly.
15. Keep demo mode and production mode clearly separated.
16. Keep the project useful for both SaaS launch preparation and professional portfolio review.

## Product honesty rules

Never make false claims.

Do not claim:

- real paying customers exist unless they actually do
- real production payments are live unless Stripe production is configured and verified
- production auth is complete unless it is actually configured and tested
- the system uses an LLM if the implementation is rule-based
- enterprise-grade security unless the relevant controls are implemented and documented
- production launch is complete while known launch drills remain unfinished

Prefer honest language:

- “SaaS MVP foundation”
- “pilot-ready demo”
- “production-shaped”
- “demo mode”
- “Stripe-ready foundation”
- “production configuration required”
- “rule-based assistant”
- “LLM extension path documented”

## RequestFlow AI MVP priorities

When asked to turn this project into a money-making SaaS product, prioritize work in this order:

1. Product repositioning from Automation Mission Control to RequestFlow AI
2. Customer-facing landing page
3. Pricing section with Free / Pro / Business
4. Public request intake form
5. Dashboard visibility for submitted requests
6. Request classification and priority suggestion
7. Internal summary and recommended next action
8. Simple onboarding / Start Free flow
9. Demo mode with sample data for first 5 customer demos
10. Billing page and Stripe checkout integration
11. Production readiness checklist
12. First pilot customer documentation

Do not jump to large, complex features before the basic customer journey works.

## UI and copy rules

Customer-facing copy should be simple, clear, and non-technical.

Use language like:

- “Collect requests in one place”
- “Turn requests into trackable work”
- “Know what is urgent”
- “Keep your team organized”
- “Start free”
- “Upgrade when your team grows”

Avoid customer-facing language like:

- “HATEOAS”
- “tenant claim”
- “idempotency key”
- “resource server”
- “bounded orchestration”
- “Flyway migration history”

Technical terms can remain in developer documentation, architecture docs, tests, and portfolio
evidence.

## Testing rules

After backend changes, run:

```bash
./mvnw clean verify
```

After frontend changes, also run:

```bash
npm ci
npx playwright install chromium
npm test
```
