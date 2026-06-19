# Production security and tenancy

The application has two deliberate security modes.

- Local development (`mission.security.enabled=false`) uses the fixed local tenant created by
  Flyway. This keeps `./mvnw spring-boot:run` and Playwright frictionless.
- Production (`SPRING_PROFILES_ACTIVE=prod`) is a stateless OAuth2 resource server. Every API
  request requires a signed JWT from the configured OIDC issuer.

## Required production claims

| Claim | Example | Purpose |
|---|---|---|
| `sub` | `auth0|123` | Immutable user identity written to audit records |
| `tenant_id` | `2c19...` | Trusted organization boundary for every query and write |
| `roles` | `["MEMBER"]` | `VIEWER`, `MEMBER`, or `ADMIN` authorization |

The API never accepts a tenant identifier in a request body or query parameter. `TenantContext`
extracts it from the verified JWT, and repository methods require it. A row owned by another
tenant returns `404`, which avoids confirming that the row exists.

## Endpoint policy

| Endpoint | Policy |
|---|---|
| `/actuator/health/**`, `/actuator/info` | Public |
| `/api/work-items/**` | Authenticated |
| `POST /api/agent/plan` | `MEMBER` or `ADMIN` |
| `POST /api/agent/runs/{id}/approve` | `ADMIN` |
| `GET /api/agent/runs` | `ADMIN` |

## Configuration

```bash
export SPRING_PROFILES_ACTIVE=prod
export OAUTH2_ISSUER_URI=https://your-tenant.example.com/
export DATABASE_URL=jdbc:postgresql://db.internal:5432/mission_control
export DATABASE_USERNAME=mission
export DATABASE_PASSWORD='from-a-secret-manager'
./mvnw spring-boot:run
```

In AWS, inject these values from Secrets Manager or Parameter Store. Do not store credentials in
CloudFormation parameters, shell history, `.env`, or CI logs.

## Verification evidence

`TenantIsolationSecurityTest` proves that unauthenticated access is rejected, roles protect agent
execution, tenant A cannot list or fetch tenant B data, and agent audit rows preserve user,
tenant, and correlation ID attribution.

This is the identity and isolation foundation—not the whole security program. Before public GA,
add browser authorization-code + PKCE login, WAF/rate limiting, secret rotation, dependency and
container scanning, backup restore drills, and an external penetration test.
