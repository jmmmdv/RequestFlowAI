# RequestFlowAI pricing (early SaaS)

Simple B2B pricing for an MVP-stage product. Limits are designed to protect margins on AI analysis
and request volume while keeping entry accessible for small teams.

**Honesty note:** The live product today includes a FREE tier and rule-based triage. The plans below
are the **target commercial packaging** for founder-led paid pilots and early customers. Align
Stripe price IDs and landing copy when you commit to these numbers.

## Plans

| Plan | Price | Best for |
|---|---|---|
| Starter | USD 29 / month | Solo operators and micro teams validating intake |
| Growth | USD 79 / month | Small agencies and service businesses with steady volume |
| Business | USD 199 / month | Teams needing higher limits and automation headroom |
| Custom / Pilot | Custom | Founder-led setup, annual prepay, or white-glove onboarding |

## Plan comparison

| Feature | Starter | Growth | Business | Custom / Pilot |
|---|---|---|---|---|
| Monthly request limit | 200 | 1,000 | 5,000 | Negotiated |
| AI analysis limit / month | 100 | 500 | 2,500 | Negotiated |
| Admin dashboard access | Yes | Yes | Yes | Yes |
| Team members | 2 | 10 | 25 | Negotiated |
| Public intake portal | 1 slug | 3 slugs | Unlimited slugs | Negotiated |
| Automation workflows | — | Basic (coming) | Advanced (coming) | Scoped in SOW |
| Email notifications | Yes | Yes | Yes | Yes |
| Priority support | Email | Email | Email + setup call | Dedicated |
| Best customer type | Freelancer, 1–2 person shop | Agency, consultant, small IT | Multi-seat ops team | Enterprise pilot, annual deal |

## What each limit protects

- **Request limit** — storage, support load, and intake abuse surface.
- **AI analysis limit** — OpenAI (or future LLM) variable cost; must stay below plan price at
  typical usage.
- **Dashboard / team** — value differentiation without unlimited free seats.
- **Automation** — priced on roadmap value; ship features before selling hard on top tiers.

## Pilot and founder-led pricing

For the first 3–5 pilots:

- Offer **Growth at Starter price** for 90 days in exchange for feedback calls and logo permission.
- Or **Custom / Pilot** with a one-time **USD 299 - 499 setup fee** plus monthly plan after go-live.
- Always document the discount and end date in writing (email or simple order form).

## Stripe implementation reminder

- Use **test mode** until checkout drill passes on hosted infrastructure.
- Create recurring Prices in Stripe for USD 29, USD 79, and USD 199.
- Webhook must update plan entitlements server-side — never trust the browser.
- Stripe processing fees are **not** included in the prices above (~2.9% + USD 0.30 per charge in US).

## Related product limits (engineering)

The codebase today enforces FREE / PRO / BUSINESS quotas (work items and agent runs). When you
adopt this pricing doc, map Stripe products to those entitlements or extend quotas to match the
request and AI limits in this table.
