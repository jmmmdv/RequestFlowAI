AGENTS.md

Mission

You are working on MissionOps AI, a commercialized version of the existing Automation Mission Control repository.

The goal is to turn this repository from a strong engineering portfolio project into a sellable first product:

MissionOps AI helps small service businesses turn messy customer or employee requests into organized, trackable, AI-assisted work with human approval and audit history.

The first sellable MVP must be simple, focused, and demo-ready.

Product Direction

Do not build a generic developer portfolio app.

Transform the app into a customer-facing SaaS product for small service businesses.

Initial niche:

* clinics
* consultants
* immigration/service offices
* small law offices
* limo/fleet/service companies
* local professional service businesses

Core customer promise:

A business receives requests from clients or staff. MissionOps AI classifies the request, creates work items, recommends next actions, requires approval for risky actions, and records an audit trail.

Current Repository Context

This repository already includes or claims support for:

* Java 21
* Spring Boot 3.5
* Spring REST APIs
* HATEOAS
* JWT / OAuth2 resource server concepts
* tenant-scoped persistence
* organizations, memberships, invitations, roles
* FREE / PRO / BUSINESS quotas
* Stripe Checkout and webhook synchronization
* bounded planning agent
* human approval for high-impact actions
* audit records with tenant, user, correlation ID, outcome, and timestamp
* Flyway migrations
* PostgreSQL profile
* H2 local development
* Testcontainers
* JUnit
* Playwright
* GitHub Actions
* Jenkins
* Docker
* AWS App Runner / RDS / CloudFormation direction
* observability with OpenTelemetry, Grafana, Prometheus, Tempo, CloudWatch
* documentation and demo scripts

Preserve the strong engineering foundation.

Commercial MVP Goal

Build only what is necessary to make the product understandable and sellable to a first customer.

The MVP must support this demo flow:

1. A business owner logs in or opens the demo.
2. They see a clear MissionOps AI dashboard.
3. A customer or employee request is submitted.
4. The system classifies the request.
5. The system assigns priority.
6. The system creates or suggests work items.
7. The system shows an AI-style recommendation.
8. Risky or high-impact actions require human approval.
9. The owner can update status.
10. The audit trail shows what happened and why.

Development Rules

General

* Make small, reviewable changes.
* Do not rewrite the whole app.
* Do not remove working tests unless replacing them with better tests.
* Do not introduce secrets into the repository.
* Keep local development simple.
* Prefer boring, reliable engineering over clever abstractions.
* Preserve existing architecture unless there is a clear reason to change it.
* Document every new environment variable.
* Keep the app demo-friendly.

Product Rules

* Every screen must make sense to a non-technical business owner.
* Avoid developer-only language in the UI.
* Replace vague terms like “agent plan” with customer-friendly labels like “AI recommendation,” “suggested next steps,” or “approval needed.”
* The product must clearly answer:
    * What request came in?
    * Who is responsible?
    * How urgent is it?
    * What does AI recommend?
    * What needs human approval?
    * What happened already?

Backend Rules

* Follow existing Spring Boot patterns.
* Keep tenant isolation strict.
* Tenant identity must come from trusted auth context, not request input.
* Validate all request bodies.
* Return structured errors.
* Keep APIs testable.
* Add or update tests for all API behavior changes.
* Keep audit records durable and attributable.
* Risky actions must remain idempotent and approval-gated.

Frontend Rules

* Keep the UI simple and business-focused.
* Do not create a complex design system unless necessary.
* Prefer clear cards, tables, status badges, and call-to-action buttons.
* The first screen should explain the product in 10 seconds.
* Make the demo data realistic for a small business.
* Avoid fake “magic AI” claims if the backend is still deterministic or rule-based.

Testing Rules

Before finishing any task, run the most relevant checks:

* Java changes: ./mvnw test or ./mvnw clean verify
* Frontend / Playwright changes: npm test
* Dependency changes: run both Java and npm checks if possible

If a command fails, diagnose it and either fix it or clearly explain the failure.

Git Rules

* Work on a branch.
* Use meaningful commits.
* Do not commit generated junk, local secrets, build artifacts, or node_modules.
* Keep pull request summaries clear:
    * What changed
    * Why it changed
    * How it was tested
    * Screenshots if UI changed

Product Vocabulary

Use these terms in customer-facing UI:

* Request
* Priority
* Status
* Recommended action
* Suggested next steps
* Approval needed
* Audit trail
* Team
* Business
* Plan
* Monthly usage

Avoid these terms in customer-facing UI unless they are in technical docs:

* agent_run
* tool budget
* tenant
* HATEOAS
* idempotency
* OAuth2 resource server
* Testcontainers
* Flyway

First MVP Features

Must Have

* MissionOps AI branding
* business-focused landing/dashboard copy
* realistic request intake form
* request list
* request detail view
* priority classification
* AI-style summary or recommendation
* status update workflow
* approval-needed state for risky actions
* audit trail visibility
* simple demo data
* updated README explaining product direction
* tests for the main flow

Should Have

* niche selector or demo presets:
    * clinic
    * consultant office
    * fleet/limo company
    * small law office
* pricing page or pricing section
* setup checklist for first customer
* improved demo script
* screenshots in docs

Not Yet

Do not build these until the MVP flow is clear:

* huge CRM
* complex calendar integration
* complex email ingestion
* advanced analytics
* mobile app
* real multi-provider AI agent marketplace
* unnecessary microservices
* unnecessary redesign

Suggested Implementation Order

1. Audit the current repository and summarize what exists.
2. Identify the fastest path to a customer-facing MVP.
3. Create a docs/product/MISSIONOPS-MVP.md plan.
4. Rename or reframe customer-facing UI copy to MissionOps AI.
5. Add realistic small-business demo data.
6. Improve request intake and dashboard flow.
7. Add AI recommendation fields if missing.
8. Add approval-needed flow if not visible enough.
9. Add audit trail visibility to the UI.
10. Add tests for the complete customer demo flow.
11. Update README and demo script.
12. Run full checks.
13. Prepare a clean PR.

Definition of Done

A task is done only when:

* the app still runs locally
* the main tests pass or failures are clearly explained
* the UI is understandable to a non-technical customer
* the README or docs are updated when behavior changes
* no secrets are committed
* the change supports the MissionOps AI MVP direction

How to Report Back

At the end of each task, respond with:

1. Summary of changes
2. Files changed
3. Commands run
4. Test results
5. Remaining risks
6. Recommended next step

Be direct. Do not hide problems.
