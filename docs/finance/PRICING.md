# RequestFlowAI pricing (MVP assumptions)

Simple B2B pricing for founder-led pilots and early customers. Limits protect margins on request
volume and future AI analysis cost.

**Honesty note:** These are **early working assumptions**. They will change after pilot feedback,
real usage data, and Stripe checkout validation. The live product today includes a FREE tier and
**rule-based** triage — not paid LLM analysis.

**Related:** [PILOT_OFFER.md](../business/PILOT_OFFER.md) · [UNIT_ECONOMICS.md](UNIT_ECONOMICS.md) ·
[PILOT_VALIDATION_PLAN.md](../business/PILOT_VALIDATION_PLAN.md)

---

## Plans

| Plan | Price | Best for |
|---|---|---|
| Starter | USD 29 / month | Solo operators and micro teams validating intake |
| Growth | USD 79 / month | Small agencies and service businesses with steady volume |
| Business | USD 199 / month | Teams needing higher limits and automation headroom |
| Pilot / Custom | Custom | Founder-led pilot, annual prepay, or scoped onboarding |

---

## Plan comparison

| Feature | Starter | Growth | Business | Pilot / Custom |
|---|---|---|---|---|
| Monthly request limit | 200 | 1,000 | 5,000 | Negotiated |
| Monthly AI analysis limit | 100 | 500 | 2,500 | Negotiated |
| Admin dashboard access | Yes | Yes | Yes | Yes |
| Basic routing / triage | Rule-based category, priority, next action | Same | Same | Scoped |
| Automation readiness | Foundation only | Basic workflows (roadmap) | Advanced workflows (roadmap) | In SOW |
| Team members | 2 | 10 | 25 | Negotiated |
| Public intake portal | 1 slug | 3 slugs | Unlimited slugs | Negotiated |
| Email notifications | Yes | Yes | Yes | Yes |
| Priority support | Email | Email | Email + setup call | Dedicated |
| Recommended customer type | Freelancer, 1–2 person shop | Agency, consultant, small IT | Multi-seat ops team | Early access partner, annual deal |

### What “AI analysis limit” means today

- Every new public intake classification can record a **zero-cost** usage event (rule-based path).
- **Paid OpenAI is disabled.** Limits are pre-provisioned for when LLM analysis ships behind
  [cost controls](../security/AI_COST_CONTROL_IMPLEMENTATION_SUMMARY.md).
- Engineering quotas today map FREE / PRO / BUSINESS differently — align Stripe entitlements when
  you commit to these commercial names.

---

## What each limit protects

| Limit | Protects |
|---|---|
| Request limit | Storage, support load, abuse surface |
| AI analysis limit | Future OpenAI variable cost per tenant |
| Dashboard / team | Value differentiation without unlimited free seats |
| Automation | Roadmap value; sell hard only after features ship |

---

## Pilot and founder-led pricing

For the first **3 pilots**, use [PILOT_OFFER.md](../business/PILOT_OFFER.md):

- **Free** or **USD 99 one-time** only — not USD 199/month until one pilot succeeds.
- Optional: Growth features at Starter price for 90 days in exchange for feedback + testimonial.
- Always document discount, end date, and plan after pilot in writing.

---

## Stripe implementation reminder

- Use **test mode** until checkout drill passes on hosted infrastructure.
- Create recurring Prices in Stripe for USD 29, USD 79, and USD 199.
- Webhook must update plan entitlements server-side — never trust the browser.
- Stripe processing fees are **not** included (~2.9% + USD 0.30 per US charge).

---

## Review after pilots

After **3 pilot customers**, update this doc with:

1. Actual requests per customer per month
2. Whether rule-based triage was “good enough”
3. Willingness to pay at each tier
4. Revised limits if margins or support load do not fit
