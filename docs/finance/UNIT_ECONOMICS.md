# RequestFlowAI unit economics

Conservative placeholder math for early planning. **All dollar figures below are assumptions** until
you have 90 days of real usage and billing data. Replace placeholders with actuals from AWS Cost
Explorer, OpenAI usage, and Stripe.

## Core definitions

| Metric | Definition |
|---|---|
| **Cost per request** | Monthly infra + email + DB alloc ÷ total requests processed |
| **Cost per AI analysis** | Monthly OpenAI spend ÷ number of AI analyses run |
| **Gross margin** | (Revenue − direct variable costs) ÷ Revenue |
| **Break-even MRR** | Fixed monthly costs ÷ gross margin % |
| **MRR** | Sum of active monthly subscriptions (excluding one-time fees) |
| **CAC** | Sales + marketing spend ÷ new paying customers in period |
| **LTV** | ARPU × gross margin % × average customer lifetime (months) |
| **Churn risk** | Probability a customer cancels in next 12 months |

## Assumptions (placeholder)

| Input | Assumed value | Notes |
|---|---|---|
| Fixed monthly costs (lean) | USD 75 | Midpoint of USD 25 - 100 budget |
| Variable cost per request (no AI) | USD 0.02 | AWS + DB amortized over 500 requests/mo |
| Variable cost per AI analysis | USD 0.08 | ~500 tokens in + out at budget API pricing |
| Starter ARPU | USD 29 | From [PRICING.md](PRICING.md) |
| Growth ARPU | USD 79 | |
| Average customer mix (early) | 70% Starter, 30% Growth | Founder-led sales |
| Monthly churn (early) | 5% | High until product stickiness proven |
| CAC (founder-led) | USD 50 | Mostly time; minimal paid ads |

## Example: one Growth customer per month

**Usage (assumed):** 400 requests, 200 AI analyses

| Line | Calculation | USD |
|---|---|---|
| Revenue | Growth plan | 79.00 |
| Request variable cost | 400 × 0.02 | 8.00 |
| AI variable cost | 200 × 0.08 | 16.00 |
| Allocated fixed cost | 75 ÷ 5 customers | 15.00 |
| **Gross profit (approx.)** | 79 − 8 − 16 − 15 | **40.00** |
| **Gross margin** | 40 ÷ 79 | **~51%** |

At 5 paying customers averaging USD 45 blended ARPU and ~50% gross margin:

- **MRR** ≈ USD 225
- **Monthly variable + fixed** ≈ USD 75 + (5 × ~USD 20 variable) ≈ USD 175
- **Approx. monthly profit** ≈ USD 50 (thin; one churn erases it)

## Break-even point (placeholder)

| Scenario | Fixed costs | Target gross margin | Break-even MRR |
|---|---|---|---|
| Lean solo founder | USD 75 / mo | 60% | USD 125 |
| Hosted pilot AWS spike | USD 150 / mo | 55% | USD 273 |
| + part-time support | USD 500 / mo | 60% | USD 833 |

**Interpretation:** First **3–4 Growth customers** or **~6 Starter customers** cover lean fixed costs
only if variable AI usage stays inside plan limits.

## CAC, LTV, and payback (placeholder)

**Example Growth customer:**

- CAC = USD 50 (founder outreach, demo time amortized)
- ARPU = USD 79, gross margin 51% → gross profit USD 40 / mo
- **CAC payback** ≈ 50 ÷ 40 ≈ **1.3 months** (good if churn stays low)
- Average lifetime at 5% monthly churn ≈ 20 months
- **LTV (rough)** = 79 × 0.51 × 20 ≈ **USD 806**
- **LTV : CAC** ≈ 16 : 1 (optimistic; churn usually worse early)

## Churn risk factors

| Risk | Mitigation |
|---|---|
| Low weekly active usage | Onboarding call; weekly digest email |
| AI quota exhaustion | Clear upgrade path; rule-based fallback |
| Cheaper spreadsheets/email | Prove time saved on first pilot |
| Single founder champion leaves | Multi-user invites; admin + member roles |
| Price shock at pilot end | Document end date; offer annual prepay discount |

## Rule-based vs AI cost (today)

The MVP uses **rule-based** classification and priority for most paths. **Cost per AI analysis**
is near **USD 0** until LLM features ship. When enabling OpenAI:

1. Start with Growth/Business quotas only.
2. Measure actual cost per analysis for 30 days.
3. Update this doc and [PRICING.md](PRICING.md) limits if margin drops below 50%.

## Monthly review template

```text
Month: ___________
Paying customers: ___
MRR: USD ___
Churned: ___
AWS spend: USD ___
OpenAI spend: USD ___
Total requests: ___
AI analyses: ___
Blended gross margin: ___%
```
