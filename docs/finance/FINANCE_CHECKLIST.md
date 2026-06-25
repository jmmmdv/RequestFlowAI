# RequestFlowAI finance checklist

Practical CFO gates for a lean solo-founder SaaS. Check items in order; do not skip “before paid
plan” gates because Stripe is already in the codebase.

## Before launch (local + docs)

- [ ] [BUDGET.md](BUDGET.md) monthly targets agreed (USD 25 - 100 safe, USD 150 max pre-revenue)
- [ ] OpenAI dashboard **hard monthly limit** set (start USD 25)
- [ ] No secrets in git — scan repo for `.env`, keys, passwords
- [ ] `./mvnw test` and `./mvnw clean package` pass on release commit
- [ ] [PRICING.md](PRICING.md) reviewed — limits match planned Stripe products
- [ ] Pilot privacy/support docs linked on public intake ([PILOT-ONBOARDING](../../src/main/resources/static/docs/pilot/ONBOARDING.md))

## Before first customer (free pilot)

- [ ] Hosted stack deployed — [PRODUCTION-PILOT-CHECKLIST.md](../saas/PRODUCTION-PILOT-CHECKLIST.md)
- [ ] Monthly AWS bill understood (Cost Explorer tag or spreadsheet row)
- [ ] Public intake smoke test pass on production API
- [ ] Cognito signup drill recorded in [DRILL-LOG.md](../saas/DRILL-LOG.md)
- [ ] Pilot agreement or email terms: free period, feedback expectation, data handling
- [ ] Support contact published ([SUPPORT.md](../../src/main/resources/static/docs/pilot/SUPPORT.md))
- [ ] `.requestflow-vercel-env.txt` values in Vercel only — not committed

## Before paid plan (first charge)

- [ ] Stripe **test** checkout drill pass; then live mode only when ready for real cards
- [ ] Stripe Prices match [PRICING.md](PRICING.md) (USD 29 / 79 / 199 or documented pilot discount)
- [ ] Webhook endpoint on HTTPS App Runner; signing secret in Secrets Manager
- [ ] Refund/cancel policy written (even if one paragraph)
- [ ] Unit economics worksheet started — [UNIT_ECONOMICS.md](UNIT_ECONOMICS.md)
- [ ] Customer knows AI analysis limits and what happens when quota is exceeded
- [ ] Bookkeeping: separate business account or tagged transactions for Stripe payouts

## Before increasing OpenAI / API limits

- [ ] Paying customer revenue covers **2×** proposed API increase
- [ ] Per-organization quotas implemented or manually enforced
- [ ] Analysis caching / no duplicate calls on refresh
- [ ] Fallback path tested (rule-based triage) when API budget hit
- [ ] 7-day usage review after limit increase

## Before spending more than USD 150 / month

- [ ] Written reason tied to revenue or signed pilot (name + date)
- [ ] Cheaper alternative documented and rejected
- [ ] Downgrade/cancel path identified
- [ ] Updated row in [PROJECTIONS.md](PROJECTIONS.md) for new burn
- [ ] If AWS: confirm RDS/App Runner sizing — no “always on” experiments left running

## Before investor or lender conversations

- [ ] 3-month actuals: MRR, churn, AWS, OpenAI (not projections only)
- [ ] [UNIT_ECONOMICS.md](UNIT_ECONOMICS.md) updated with real CAC and margin
- [ ] [PROJECTIONS.md](PROJECTIONS.md) marked with actuals vs plan variance
- [ ] Honest product status — rule-based vs AI, pilot count, drill log
- [ ] Cap table / founder ownership clear (even if 100% founder)
- [ ] No secrets in pitch deck, demo video, or screen recordings
- [ ] Portfolio vs commercial claims separated ([CASE-STUDY.md](../portfolio/CASE-STUDY.md))

## Monthly recurring review (15 minutes)

1. Record MRR and customer count in [PROJECTIONS.md](PROJECTIONS.md) appendix or spreadsheet.
2. Log AWS + OpenAI + Vercel + email bills against [BUDGET.md](BUDGET.md).
3. If burn > USD 150 with MRR < USD 145, trigger spend freeze per [FINANCE_STRATEGY.md](FINANCE_STRATEGY.md).
4. One customer conversation about willingness to pay at list price.

## Quick reference: spend freeze triggers

Stop discretionary spend immediately if any occur:

- OpenAI bill exceeds cap two months in a row
- AWS > USD 150 with zero paying customers
- Churn removes >20% of MRR in one month
- No pilot activity for 30 days while infra stays up — tear down or pause stack
