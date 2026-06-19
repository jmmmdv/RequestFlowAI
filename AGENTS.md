# AGENTS.md

## Project identity

This repository is **FromZeroToHero / Automation Mission Control**.

It is a production-shaped portfolio capstone for learning and demonstrating:

- Java backend development
- Spring Boot REST APIs
- SaaS-style multi-tenancy
- security and auditability
- automated testing
- browser automation
- CI/CD
- AWS deployment concepts
- UiPath / RPA integration concepts
- AI agents and agentic AI guardrails
- intelligent automation
- real-world software delivery practices

This is not a random tutorial repository. Every change should strengthen the project as a job-readiness evidence project.

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

## Working rules for Codex

Before making changes:

1. Read `README.md`.
2. Read this `AGENTS.md`.
3. Inspect the relevant source and test files.
4. Prefer small, safe, reviewable changes.
5. Do not rewrite the architecture unless explicitly asked.
6. Do not delete existing tests to make the build pass.
7. Do not weaken security, tenant isolation, auditability, idempotency, or coverage gates.
8. Do not claim a feature is complete unless the code or documentation proves it.
9. If a feature is only a lab, roadmap item, or planned extension, label it clearly.
10. Keep the project useful for both learning and professional portfolio review.

## Test commands

After backend changes, run:

```bash
./mvnw clean verify
```
