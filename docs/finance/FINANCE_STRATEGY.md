# RequestFlowAI finance strategy

RequestFlowAI is a lean B2B SaaS product for collecting business requests, analyzing them (rule-based
today with a documented path to AI-assisted analysis), routing them to the right team, and tracking
them in an admin dashboard. Automation workflows are planned; the first financial goal is not scale
— it is proof that customers want the product while costs stay controlled and predictable.

## Early financial principles

1. **Validate before you scale.** Do not increase infrastructure, headcount, or feature surface until
   pilot customers show repeat usage and willingness to pay.
2. **Free tiers first.** Use professional free or low-cost tiers (Vercel, AWS sandbox patterns,
   Stripe test mode) until revenue justifies upgrade.
3. **Cap variable AI spend.** OpenAI and similar APIs are the fastest way to blow a pre-revenue
   budget. Hard monthly limits, per-plan quotas, and result caching are mandatory.
4. **Price on value drivers.** Plans should align with request volume, AI analysis usage, dashboard
   seats/features, and automation — not vanity metrics.
5. **One founder, one ledger.** Track every recurring line item monthly. No “we’ll figure it out at
   tax time.”
6. **Honest product claims.** Finance assumptions must match what the product actually ships. Do not
   budget for LLM scale if the live path is still rule-based unless you have a dated rollout plan.

## Main finance areas

| Area | Document | Purpose |
|---|---|---|
| Monthly burn | [BUDGET.md](BUDGET.md) | Cost targets and spending rules |
| Customer pricing | [PRICING.md](PRICING.md) | Plans, limits, and positioning |
| Per-customer math | [UNIT_ECONOMICS.md](UNIT_ECONOMICS.md) | Margin, break-even, CAC, LTV |
| Forward view | [PROJECTIONS.md](PROJECTIONS.md) | 3 / 6 / 12 / 24 month scenarios |
| Operating gates | [FINANCE_CHECKLIST.md](FINANCE_CHECKLIST.md) | CFO checkpoints before spend |

## Current CFO strategy

**Phase:** Pre-revenue / MVP validation with founder-led pilots.

**Priority order:**

1. Keep monthly cash burn inside **USD 25 - 100** (hard cap **USD 150** before meaningful revenue).
2. Run hosted free pilots to prove intake, triage, and dashboard value — not to impress investors
   with infrastructure spend.
3. Introduce paid plans only after at least one pilot completes onboarding and gives usable feedback.
4. Defer expensive monitoring, multi-region, and heavy automation until **USD 1,000+ MRR** or
   equivalent annual contracts.

The first goal is not to look like a big company. The first goal is to prove that customers want
RequestFlowAI while keeping costs controlled, predictable, and reversible if demand is weak.

## Spending discipline rules

Before any new recurring charge, answer all four:

1. Does this help us get or retain a paying customer?
2. Does it remove a real operational blocker for pilots?
3. Is there a cheaper way to test the same hypothesis this week?
4. Can we cancel or downgrade within 30 days without data loss?

**Default no** for: paid ads at scale, premium observability suites, multiple environments beyond
dev + one pilot stack, contractor spend without a scoped deliverable, and OpenAI tier upgrades
without per-customer revenue attached.

**Default yes** for: domain renewal, minimal AWS/Vercel to keep pilots online, Stripe (fee-only),
and email on free tier for intake notifications.

## Secret protection reminder

Never commit to GitHub (or any public repo):

- OpenAI API keys
- AWS access keys or session tokens
- Database passwords or connection strings with credentials
- Stripe secret keys or webhook signing secrets
- Cognito client secrets (if used)
- `.env`, `.env.local`, or credential JSON files

Use environment variables, AWS Secrets Manager, Vercel encrypted env, and Stripe Dashboard for
secrets. Rotate anything that was ever pasted into chat, email, or a screenshot.

See also [AGENTS.md](../../AGENTS.md) engineering rules and
[docs/saas/PRODUCTION-PILOT-CHECKLIST.md](../saas/PRODUCTION-PILOT-CHECKLIST.md) for production
configuration boundaries.
