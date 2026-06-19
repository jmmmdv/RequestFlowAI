# Job-search kit

Copy-paste content built from this project. Replace `_placeholders_` before using. Keep every
claim honest — it all maps to working code and tests in this repo.

---

## LinkedIn headline (pick one)

- Java / Spring Boot Engineer · Multi-tenant SaaS, secure REST APIs, AWS, test-driven delivery
- Backend Engineer (Java 21, Spring Boot) · SaaS multi-tenancy · billing · bounded AI automation
- Software Engineer · Spring Boot SaaS · OAuth2 security · CI/CD · AWS · Stripe

## LinkedIn "About" snippet

> I build production-shaped backends, not demos. My capstone, Automation Mission Control, is a
> multi-tenant SaaS (Java 21, Spring Boot, PostgreSQL) with JWT-backed tenant isolation, role-based
> access, Stripe billing with signature-verified webhooks, and a bounded automation agent that
> keeps high-impact actions behind human approval. Every layer is tested — security, API/DB
> consistency, PostgreSQL via Testcontainers, and Playwright end-to-end — behind a single delivery
> gate with an 80% coverage floor, deployable to AWS via CloudFormation. I care about safety,
> auditability, and shipping software a team can actually operate.

## LinkedIn "Featured" caption

> **Automation Mission Control — multi-tenant SaaS + bounded automation agent**
> Java 21 · Spring Boot · PostgreSQL · OAuth2 · Stripe · Playwright · AWS. Tenant-isolated security
> (proven by tests), human-in-the-loop agent with full audit trail, and a one-command delivery gate.
> Case study and 5-minute demo linked. Live demo: _add your URL_ · Code: _repo URL_

---

## Resume — Projects section entry

**Automation Mission Control** — _solo project_ · Java 21, Spring Boot, PostgreSQL, AWS, Stripe
_(github.com/jmmmdv/FromZeroToHero)_

- Designed and built a multi-tenant SaaS REST API (Spring Web + HATEOAS) with validation and
  RFC 9457 problem responses, enforcing tenant identity from verified JWT claims at the repository
  layer; automated security tests prove cross-organization data cannot be listed or fetched.
- Implemented a bounded planning agent that classifies goals, enforces policy and tool-call
  budgets, requires idempotent human approval for high-impact actions, and writes an attributable
  audit record (tenant, user, correlation ID, outcome) per run; behavior gated by a 27-case
  golden/adversarial evaluation suite in CI.
- Shipped a SaaS control plane — organizations, roles, expiring invitations, FREE/PRO/BUSINESS
  quotas, and Stripe Checkout with constant-time, replay-resistant webhook verification.
- Established one delivery gate (unit, integration, security, contract, PostgreSQL Testcontainers,
  Playwright E2E) with an 80% JaCoCo coverage floor; one Flyway migration history runs on H2, CI,
  and RDS.
- Automated build and deploy with GitHub Actions and Jenkins, containerized with Docker, and
  provisioned AWS (App Runner, private RDS, Secrets Manager) via CloudFormation with OpenTelemetry,
  Grafana, and CloudWatch observability.

## Resume — one-line version (for a skills/summary line)

- Built a production-shaped multi-tenant SaaS (Spring Boot, PostgreSQL, AWS) with tested tenant
  isolation, Stripe billing, a human-in-the-loop automation agent, and an 80%-coverage CI gate.

---

## Outreach templates

### Recruiter / hiring-manager DM

> Hi _name_ — I'm a backend engineer focused on Java/Spring Boot and SaaS. I recently built a
> multi-tenant SaaS with tested tenant isolation, Stripe billing, a human-in-the-loop automation
> agent, and an 80%-coverage delivery gate deployable to AWS. Two-minute case study here: _link_.
> I'd love to talk about _role/team_ — open to a quick chat?

### Application note / cover blurb

> I'm applying for _role_. Rather than describe my experience abstractly, I'll point to working
> software: Automation Mission Control is a multi-tenant Spring Boot SaaS I designed, built, tested,
> and prepared for AWS deployment. It demonstrates the things this role needs — secure
> multi-tenancy, REST API design, billing, CI/CD, and safe automation — with tests proving each
> claim. Case study: _link_. I'd welcome the chance to walk through any layer.

---

## ATS keywords to weave in naturally

Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security, OAuth2, JWT, REST API, HATEOAS,
multi-tenancy, RBAC, PostgreSQL, Flyway, Testcontainers, JUnit, Playwright, CI/CD, GitHub Actions,
Jenkins, Docker, AWS, App Runner, RDS, CloudFormation, Stripe, observability, OpenTelemetry, Grafana.

---

## Three rules for using this kit

1. **Be honest.** The agent is rule-based (not an LLM) and the public site is a portfolio preview;
   say so if asked. Senior engineers respect accurate scope.
2. **Lead with evidence.** Always link the [case study](CASE-STUDY.md) and the recorded demo.
3. **Tailor per application.** Mirror the role's language (e.g. "platform", "billing", "automation")
   using the keyword list above.
