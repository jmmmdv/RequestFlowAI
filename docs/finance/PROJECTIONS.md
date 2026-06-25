# RequestFlowAI financial projections

Conservative scenarios for a founder-led B2B SaaS MVP. **Not a forecast guarantee** — use for
planning and spend gates. Assumes organic pilots, no paid acquisition at scale, and monthly costs
controlled per [BUDGET.md](BUDGET.md).

## Shared assumptions

| Assumption | Value |
|---|---|
| Starting MRR | USD 0 |
| New paying customers / month (avg.) | See horizon tables |
| Blended ARPU | USD 45 (mix of Starter USD 29 and Growth USD 79) |
| Monthly churn | 4–6% once paying base exists |
| Base monthly costs (no revenue) | USD 75 |
| Costs with 1 hosted pilot stack | USD 120 - 150 |
| OpenAI cap | USD 25 / mo until 5+ paying customers |

## 3-month projection (validation)

**Story:** Free pilots, first paid pilot or 1–2 Starter customers by month 3.

| Month | Paying customers | MRR (USD) | Monthly cost (USD) | Est. profit / loss (USD) |
|---|---:|---:|---:|---:|
| 1 | 0 | 0 | 120 | −120 |
| 2 | 0 | 0 | 120 | −120 |
| 3 | 2 | 58 | 130 | −72 |

**Cumulative 3-month loss (approx.):** USD −312

**Milestones:** Hosted free pilot live; 2 pilots completed feedback calls; Stripe test checkout
drill pass optional before charging.

## 6-month projection (early revenue)

**Story:** 1–2 new paying customers per month; slow churn on early adopters.

| Month | Paying customers | MRR (USD) | Monthly cost (USD) | Est. profit / loss (USD) |
|---|---:|---:|---:|---:|
| 4 | 3 | 87 | 130 | −43 |
| 5 | 5 | 145 | 135 | 10 |
| 6 | 7 | 203 | 140 | 63 |

**Month 5–6:** Approaching operational break-even on lean costs if AWS stays capped.

**Milestones:** 5 paying customers OR USD 200+ MRR; first Growth customer; unit economics worksheet
updated with real AWS and OpenAI bills.

## 12-month projection (sustainable side business)

**Story:** Modest growth; no hiring; automation features in beta on Business tier.

| Quarter | End customers | MRR (USD) | Avg monthly cost (USD) | Quarterly profit / loss (USD) |
|---|---:|---:|---:|---:|
| Q1 (mo 1–3) | 2 | 58 | 123 | −312 |
| Q2 (mo 4–6) | 7 | 203 | 135 | −30 |
| Q3 (mo 7–9) | 12 | 348 | 150 | 144 |
| Q4 (mo 10–12) | 18 | 522 | 165 | 291 |

**End of month 12 (approx.):**

- **MRR:** USD 522
- **ARR run-rate:** ~USD 6,300
- **Annual costs:** ~USD 1,800
- **Annual net (before tax):** ~USD 4,500 (highly sensitive to churn and AWS)

## 24-month projection (controlled growth)

**Story:** Word-of-mouth and founder sales only; raise prices only with added automation value.

| Milestone | Month | Paying customers | MRR (USD) | Monthly cost (USD) | Est. profit / loss (USD) |
|---|---|---:|---:|---:|---:|
| Break-even steady state | 9 | 12 | 348 | 150 | 48 |
| First USD 500 MRR | 14 | 16 | 504 | 170 | 184 |
| First USD 1,000 MRR | 22 | 28 | 1,036 | 220 | 556 |
| Month 24 | 24 | 32 | 1,188 | 250 | 446 |

**Month 24 (approx.):**

- **MRR:** USD 1,188
- **ARR run-rate:** ~USD 14,300
- **Monthly costs:** USD 250 (slightly higher email, AWS, OpenAI with more analyses)
- **Not assumed:** full-time salary, paid ads, second engineer

## What would change these numbers

| Upside | Downside |
|---|---|
| Annual prepay (2 months free) | Pilot stack left running without revenue |
| Custom pilot setup fees USD 299+ | OpenAI limits not enforced per tenant |
| 2–3 agency referrals | >5% monthly churn |
| Business tier at USD 199 | AWS spend uncapped beyond USD 150 pre-revenue |

## CFO use

Review this file quarterly. If actual MRR trails the 6-month table by more than 40% for two
consecutive months, **freeze** new spend and return to free pilots until positioning or pricing is
fixed.
