# External drill evidence log

Append-only record of **live** drills against sandbox or production-shaped infrastructure.
CI and `./mvnw verify` are necessary but **not** sufficient evidence for this log.

| Drill ID | Description | Status | Last run (UTC) | Environment | Operator | Notes |
|---|---|---|---|---|---|---|
| `cognito-signup` | Cognito signup + Start Free onboarding | **pass** | 2026-06-19 20:21 UTC | production us-east-1 | jeyhun | Founder onboarded; Cognito tenant_id claim + workspace setup completed after PostConfirmation fix |
| `cognito-invitation-transfer` | Invitation accept after Cognito tenant transfer | **pending** | — | — | — | — |
| `stripe-checkout` | Stripe test Checkout + webhook plan sync | **pending** | — | — | — | — |
| `aws-restore` | RDS point-in-time restore drill | **pending** | — | — | — | — |

## Completed runs
### 2026-06-19 20:13 UTC — `cognito-signup` (PASS)
- Environment: production us-east-1
- Operator: jeyhun
- Notes: Founder onboarded; Cognito tenant_id + workspace setup completed


Use `./scripts/drills/record-drill.sh` after each successful drill. Example:

```bash
./scripts/drills/record-drill.sh cognito-signup pass \
  --env "sandbox us-east-1" \
  --operator "your-name" \
  --notes "JWT tenant_id matched organization API"
```

### 2026-06-19 20:21 UTC — `cognito-signup` (PASS)
- Environment: production us-east-1
- Operator: jeyhun
- Notes: Founder onboarded; Cognito tenant_id claim + workspace setup completed after PostConfirmation fix

