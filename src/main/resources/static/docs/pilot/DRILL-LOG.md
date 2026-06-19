# External drill evidence log

Append-only record of **live** drills against sandbox or production-shaped infrastructure.
CI and `./mvnw verify` are necessary but **not** sufficient evidence for this log.

Full runbook: [EXTERNAL-DRILLS.md](https://github.com/jmmmdv/FromZeroToHero/blob/main/docs/saas/EXTERNAL-DRILLS.md) (or `/docs/saas/EXTERNAL-DRILLS.md` in the repository).

| Drill ID | Description | Status | Last run (UTC) | Environment | Operator | Notes |
|---|---|---|---|---|---|---|
| `cognito-signup` | Cognito signup + Start Free onboarding | **pending** | — | — | — | — |
| `cognito-invitation-transfer` | Invitation accept after Cognito tenant transfer | **pending** | — | — | — | — |
| `stripe-checkout` | Stripe test Checkout + webhook plan sync | **pending** | — | — | — | — |
| `aws-restore` | RDS point-in-time restore drill | **pending** | — | — | — | — |

## Completed runs

_No live drills recorded yet._

Record from the repository root:

```bash
./scripts/drills/record-drill.sh cognito-signup pass \
  --env "sandbox us-east-1" \
  --operator "your-name" \
  --notes "JWT tenant_id matched organization API"
```
