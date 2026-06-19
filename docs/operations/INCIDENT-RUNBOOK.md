# Incident response runbook

## Severity and ownership

| Severity | Definition | Initial response |
|---|---|---:|
| SEV-1 | complete outage, suspected breach, or tenant isolation failure | 5 minutes |
| SEV-2 | elevated errors/latency or a major feature unavailable | 15 minutes |
| SEV-3 | degraded non-critical behavior with a workaround | next business day |

The first responder is incident commander until a named replacement accepts ownership. Create a
timeline immediately; record facts and decisions, never secrets or raw access tokens.

## First 15 minutes

1. Acknowledge the SNS alarm and open the `automation-mission-control` CloudWatch dashboard.
2. Confirm impact with `/actuator/health/readiness`; do not rely on one customer report.
3. Correlate App Runner 5xx/latency, deployment events, RDS CPU/storage/connections, and trace IDs.
4. Stop the bleeding: roll back the image, disable the failing feature, or scale capacity.
5. If tenant isolation or credentials may be compromised, declare SEV-1, preserve evidence, and
   rotate credentials through Secrets Manager. Do not announce “contained” until verified.
6. Send a status update containing impact, mitigation, owner, and next update time.

## Fast diagnosis

```bash
aws apprunner list-operations --service-arn "$SERVICE_ARN"
aws cloudwatch describe-alarms --alarm-name-prefix automation-mission-control
aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_ID"
aws logs tail "$APP_LOG_GROUP" --since 30m --follow
```

- Errors rose immediately after deployment: roll back to the previous immutable ECR digest.
- Readiness is down and RDS connections are saturated: reduce load, inspect slow queries, and
  avoid restarting every instance simultaneously.
- Free storage alarm: stop nonessential writes, investigate growth, then increase allocated storage.
- Only one tenant is affected: use tenant ID plus correlation/trace ID; never query another tenant
  from a customer-facing endpoint.

## Recovery and closure

Verify health, one authenticated CRUD journey, tenant isolation, and an agent approval journey.
Watch the dashboard for 15 minutes before resolving. Within two business days, record root cause,
customer impact, detection gap, contributing conditions, and prevention work with named owners.
