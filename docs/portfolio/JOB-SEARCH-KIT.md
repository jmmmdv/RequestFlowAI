# RequestFlow AI portfolio and job-search kit

Copy-paste material built from working code and tests in this repository. Replace placeholders and
keep every claim within the evidence. The project is both a SaaS MVP foundation and an engineering
portfolio; neither side needs to hide the other.

## LinkedIn headline options

- Java / Spring Boot Engineer · Multi-tenant SaaS, secure REST APIs, AWS, test-driven delivery
- Backend Engineer · Java 21 · SaaS multi-tenancy · Stripe · bounded automation · PostgreSQL
- Software Engineer building RequestFlow AI · Spring Boot · OAuth2 · CI/CD · AWS

## LinkedIn About snippet

> I build production-shaped Java backends around real product problems. My current project,
> RequestFlow AI, is a SaaS MVP foundation for small service teams that need to turn incoming
> requests into trackable work. The Spring Boot backend includes JWT-backed tenant isolation,
> role-based access, organizations and invitations, plan quotas, Stripe Checkout with verified
> webhooks, and a rule-based planning assistant with human approval and audit history. Security,
> API/database consistency, PostgreSQL migrations, and browser journeys are automated behind an
> 80% coverage gate. It is a working pilot-ready demo with a documented AWS path—not a claim of a
> completed commercial launch.

## LinkedIn Featured caption

> **RequestFlow AI — SaaS request-management foundation + production-shaped Java backend**
> Java 21 · Spring Boot · PostgreSQL · OAuth2 · Stripe · Playwright · AWS. Public request intake,
> rule-based triage, trackable work,
> tenant-isolated data, team roles, quotas, verified billing events, and bounded automation with a
> full audit trail. Case study: _link_ · Demo: _link_ ·
> Code: https://github.com/jmmmdv/RequestFlowAI

## Resume project entry

**RequestFlow AI** — SaaS MVP foundation, solo project · Java 21, Spring Boot, PostgreSQL, AWS,
Stripe · [github.com/jmmmdv/RequestFlowAI](https://github.com/jmmmdv/RequestFlowAI)

- Designed and built a tenant-scoped request/work management REST API with validation, HATEOAS,
  status tracking, and RFC 9457-style problem responses; verified JWT identity supplies the tenant
  boundary and security tests prove cross-organization data cannot be listed or fetched.
- Implemented idempotent public intake that resolves tenant ownership server-side, preserves the
  original request, applies deterministic category/priority/summary/next-action rules, and creates
  quota-checked work visible in the authenticated dashboard.
- Implemented a deterministic planning assistant with classification rules, tool budgets,
  idempotent human approval for high-impact actions, and attributable audit records; gated behavior
  with a 27-case golden/adversarial evaluation suite.
- Built a SaaS control plane with organizations, memberships, roles, expiring invitations,
  FREE/PRO/BUSINESS quotas, Stripe Checkout, and replay-resistant signature verification for
  webhook-driven subscription state.
- Established one delivery gate spanning unit, integration, security, contract, PostgreSQL
  Testcontainers, Playwright E2E, and an 80% JaCoCo coverage floor; used one Flyway migration
  history across H2 and PostgreSQL.
- Declared an AWS deployment path with Docker, App Runner, private encrypted RDS, Secrets Manager,
  Cognito, OpenTelemetry, Grafana, CloudWatch, SLOs, and operational runbooks.

## One-line version

- Built RequestFlow AI, a production-shaped Spring Boot SaaS foundation with tested tenant
  isolation, work tracking, Stripe-ready billing, bounded automation, and an 80%-coverage CI gate.

## Recruiter or hiring-manager message

> Hi _name_ — I am a Java/Spring Boot engineer building RequestFlow AI, a SaaS MVP foundation for
> turning small-business requests into trackable work. The working backend demonstrates tested
> multi-tenant isolation, organizations and roles, Stripe-ready billing, bounded automation, and a
> production-shaped AWS path. Case study: _link_. I would enjoy discussing how that evidence maps
> to _role/team_.

## Application note

> I am applying for _role_. RequestFlow AI is the clearest example of how I work: I started with a
> customer problem, built a tenant-safe Spring Boot SaaS foundation, and backed the security,
> billing, automation, and data claims with tests and operational documentation. It is honest about
> its current boundary—a pilot-ready foundation, not a completed launch. Case study: _link_. I would
> welcome the chance to walk through any layer.

## Founder or pilot-customer description

> RequestFlow AI is being built for small service teams that lose client or internal requests
> across email, messages, calls, and forms. The goal is one simple place to collect requests, know
> what is urgent, turn them into work, and track status. The current demo proves the team workspace,
> safety, and SaaS foundation, including a working public request-to-work journey. I am looking for
> pilot feedback and completing production abuse/privacy controls before claiming commercial launch.

## ATS keywords

Java 21, Spring Boot, Spring Web, Spring Data JPA, Spring Security, OAuth2, JWT, REST API, HATEOAS,
multi-tenancy, RBAC, PostgreSQL, Flyway, Testcontainers, JUnit, Playwright, CI/CD, GitHub Actions,
Jenkins, Docker, AWS, App Runner, RDS, CloudFormation, Stripe, OpenTelemetry, Grafana.

## Rules for using this kit

1. Say **SaaS MVP foundation**, **pilot-ready demo**, and **rule-based assistant** until stronger
   evidence exists.
2. Never claim paying customers, live production billing, LLM usage, or completed production drills
   without proof.
3. Lead with the RequestFlow AI customer problem, then link the engineering evidence.
4. Tailor the emphasis to the audience: customer workflow for pilots, system design and tests for
   engineering roles.
