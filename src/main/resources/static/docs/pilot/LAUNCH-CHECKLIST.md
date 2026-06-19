# Production pilot launch checklist

Record the date and result when each drill is actually executed. Do not check an item until evidence exists.

**External drills runbook:** [EXTERNAL-DRILLS.md](EXTERNAL-DRILLS.md)  
**Evidence log:** [DRILL-LOG.md](DRILL-LOG.md)

## Public intake protection

- [x] Rate limiting, honeypot, retention, and portal tokens implemented in code
- [ ] Enable `mission.intake.rate-limit.enabled=true` in production (or equivalent WAF limits)
- [ ] Verify honeypot on the live public form
- [ ] Configure retention days for pilot organization
- [ ] Publish [PRIVACY.md](PRIVACY.md) beside the public form

## Identity and onboarding

- [ ] Cognito signup creates tenant claim and founder ADMIN membership — [drill 1](../saas/EXTERNAL-DRILLS.md#drill-1--cognito-signup-and-start-free-onboarding)
- [ ] **Start free** completes workspace setup (name + slug)
- [ ] Invitation accept works for matching email
- [ ] Invitation transfer drill updates Cognito tenant attribute and groups — [drill 2](../saas/EXTERNAL-DRILLS.md#drill-2--team-invitation-and-cognito-tenant-transfer)

## Billing

- [ ] Stripe test Checkout upgrades a workspace — [drill 3](../saas/EXTERNAL-DRILLS.md#drill-3--stripe-test-checkout-and-webhook)
- [ ] Signed webhook updates plan and quota limits
- [ ] Cancelled subscription returns workspace to FREE limits

## Operations

- [ ] AWS restore drill completed — [drill 4](../saas/EXTERNAL-DRILLS.md#drill-4--aws-postgresql-restore)
- [ ] Health checks and alerts configured for API and database
- [ ] Pilot [ONBOARDING.md](ONBOARDING.md) and [SUPPORT.md](SUPPORT.md) shared with first customer

## Pilot validation

- [ ] First pilot completed a full request → inbox → work → status journey
- [ ] Feedback captured and prioritized for next iteration
