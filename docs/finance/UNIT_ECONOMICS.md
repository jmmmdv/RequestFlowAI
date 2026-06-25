# RequestFlowAI unit economics

Simple planning math for a lean pre-revenue SaaS. **Replace assumptions with real data** after 90
days of pilots, AWS bills, and Stripe revenue.

**Related:** [PRICING.md](PRICING.md) · [BUDGET.md](BUDGET.md) ·
[PILOT_VALIDATION_PLAN.md](../business/PILOT_VALIDATION_PLAN.md) ·
[AI cost control summary](../security/AI_COST_CONTROL_IMPLEMENTATION_SUMMARY.md)

---

## Current state (important)

| Topic | Value |
|---|---|
| Paid AI (OpenAI) | **Disabled** — USD 0 variable AI spend today |
| Request analysis | **Rule-based** — near-zero marginal cost per classification |
| AI cost controls | Budget, usage ledger, and quotas **implemented** but not driving live paid calls |
| Target gross margin | **80%+** once paid AI is enabled and priced into plans |
| Early monthly infrastructure | **USD 25–100** target band |
| Maximum spend before revenue | **USD 150 / month** hard cap |

When paid AI is enabled, **every analysis must be tracked** in `ai_usage_event` and checked against
global budget and per-plan quota before calling the provider.

---

## Core definitions

| Metric | Definition |
|---|---|
| **Cost per request** | Monthly infra + email + DB alloc ÷ total requests processed |
| **Cost per AI analysis** | Monthly OpenAI spend ÷ number of paid AI analyses (USD 0 today) |
| **Gross margin** | (Revenue − direct variable costs) ÷ Revenue |
| **Break-even MRR** | Monthly fixed costs ÷ gross margin % (simplified) |
| **MRR** | Sum of active monthly subscriptions |
| **CAC** | Sales + marketing spend ÷ new paying customers in period |
| **LTV** | ARPU × gross margin % × average customer lifetime (months) |

---

## Assumptions (early)

| Input | Assumed value | Notes |
|---|---|---|
| Fixed monthly costs (lean) | USD 50–75 | Midpoint of USD 25–100 infrastructure target |
| Variable cost per request (no paid AI) | USD 0.01–0.03 | AWS + DB amortized over volume |
| Variable cost per paid AI analysis | USD 0.00 **today**; budget USD 0.05–0.10 when enabled | Measure 30 days after enablement |
| Starter ARPU | USD 29 | [PRICING.md](PRICING.md) |
| Growth ARPU | USD 79 | |
| Business ARPU | USD 199 | |
| Target gross margin (steady state) | **80%+** | Requires AI cost inside plan limits |
| Early gross margin (rule-based only) | Often **70–90%** | Thin fixed-cost base; don’t over-interpret |

---

## Break-even examples (simplified)

These examples cover **lean fixed costs only** (~USD 75–150/month). They ignore founder salary,
paid ads, and Stripe fees. Use as directional targets, not promises.

| Scenario | Customers | Price | MRR | Covers ~USD 150/mo fixed? |
|---|---|---|---|---|
| Business-heavy | 2 × Business | USD 199 | USD 398 | Yes |
| Growth mix | 5 × Growth | USD 79 | USD 395 | Yes |
| Starter volume | 14 × Starter | USD 29 | USD 406 | Yes |

**Interpretation:**

- You do not need hundreds of customers to cover **lean infra** — you need a **small set of paying
  pilots** at Growth or Business price.
- At Starter-only mix, support time can erase margin before AWS does.
- **Do not enable paid AI** until gross margin at real usage still trends toward **80%+** after AI
  cost per analysis is known.

---

## Example: one Growth customer (today — no paid AI)

**Usage (assumed):** 400 requests, 200 analyses (rule-based, zero AI API cost)

| Line | Calculation | USD |
|---|---|---|
| Revenue | Growth plan | 79.00 |
| Request variable cost | 400 × 0.02 | 8.00 |
| AI variable cost | Paid AI disabled | 0.00 |
| Allocated fixed cost | 75 ÷ 5 customers | 15.00 |
| **Gross profit (approx.)** | 79 − 8 − 0 − 15 | **56.00** |
| **Gross margin** | 56 ÷ 79 | **~71%** |

When paid AI ships at USD 0.08/analysis and 200 paid analyses:

| Line | USD |
|---|---|
| AI variable cost | 16.00 |
| Gross profit | 40.00 |
| Gross margin | ~51% |

That is **below** the 80% target — either raise price, tighten AI limits, cache results, or improve
model efficiency before broad rollout.

---

## Future paid AI checklist

Before assuming USD 0.08/analysis in pricing:

1. Enable provider only behind `AiBudgetService` and `AiAnalysisQuotaService`
2. Run 30 days on Growth/Business pilots with usage logging
3. Compute **actual** cost per analysis from `ai_usage_event`
4. Update [PRICING.md](PRICING.md) limits if margin &lt; 80%

---

## CAC, LTV, and payback (placeholder)

**Example Growth customer (rule-based era):**

- CAC = USD 50 (founder outreach, demo time)
- ARPU = USD 79, gross margin ~71% → gross profit ~USD 56 / mo
- **CAC payback** ≈ 1 month (good if churn stays low)
- At 5% monthly churn, lifetime ≈ 20 months → **LTV (rough)** ≈ USD 790
- Early churn is usually worse — treat LTV as optimistic until 6 months of data

---

## Churn risk factors

| Risk | Mitigation |
|---|---|
| Low weekly dashboard usage | Onboarding call; weekly digest |
| “We thought AI was included” | Honest positioning; rule-based today |
| Cheaper spreadsheets/email | Prove one missed request avoided |
| Price shock at pilot end | Document end date in [PILOT_OFFER.md](../business/PILOT_OFFER.md) |
| AI quota exhaustion later | Upgrade path; rule-based fallback |

---

## Monthly review template

```text
Month: ___________
Paying customers: ___
MRR: USD ___
Churned: ___
AWS spend: USD ___
OpenAI spend: USD ___  (expect 0 while disabled)
Total requests: ___
Analyses recorded: ___
Paid AI analyses: ___
Blended gross margin: ___%
Pilot learnings: ___
```
