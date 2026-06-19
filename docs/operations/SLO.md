# Service-level objectives

These objectives turn “the service is healthy” into measurable promises. They apply to the
production API and exclude planned maintenance announced at least 24 hours in advance.

| Indicator | Objective | Measurement | Alert |
|---|---:|---|---|
| Availability | 99.9% successful requests over 30 days | non-5xx / all App Runner requests | ≥5 server errors in 5 minutes |
| Latency | 95% of requests below 1 second over 30 days | App Runner `RequestLatency` | average >1 second for 3 of 5 minutes |
| Durability | RPO ≤24 hours | latest successful RDS backup | backup event missing or failed |
| Recovery | RTO ≤60 minutes | timed quarterly restore drill | restore drill exceeds 60 minutes |

The 99.9% availability target permits about 43 minutes of unplanned failure per 30-day window.
At 50% error-budget consumption, pause risky releases and prioritize reliability work. At 100%,
freeze feature releases until the contributing failure mode is corrected and tested.

CloudWatch is the production source of truth. The local Grafana dashboard uses the same golden
signals—traffic, errors, latency, saturation—so an interviewer can exercise the operating model
without an AWS account.

## Review cadence

- Weekly: review alarms, p95 latency, database saturation, and error-budget consumption.
- Monthly: record the SLO result and the three largest reliability risks.
- Quarterly: run `RESTORE-DRILL.md` and one incident simulation.
- After every severity-1/2 incident: publish a blameless review with owners and due dates.
