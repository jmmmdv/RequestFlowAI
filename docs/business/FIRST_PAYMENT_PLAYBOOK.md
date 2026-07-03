# First payment playbook

How to collect money **before** in-app Stripe Checkout is live on production. No secrets in this
doc — you add payment links and accounts in your own Stripe/PayPal dashboards.

**Current state:** Production App Runner stack has **empty** Stripe parameters. Dashboard **Upgrade**
buttons will not complete checkout until `setup-stripe-sandbox.sh` (or production Stripe secrets) is
done and drill `stripe-checkout` is recorded. **Manual payment is the fastest path to USD 99 today.**

**Related:** [REVENUE_SPRINT_14_DAYS.md](REVENUE_SPRINT_14_DAYS.md) · [PILOT_OFFER.md](PILOT_OFFER.md)

---

## Option 1 — Stripe Payment Link (recommended)

No code changes. Works in **test mode** for practice; switch to **live mode** when ready for real money.

1. Log in to [Stripe Dashboard](https://dashboard.stripe.com)
2. **Products** → create product **RequestFlowAI Pilot — 30 days**
3. Add price **USD 99** one-time
4. **Payment Links** → create link → copy URL
5. Send link after the demo: “Here is the pilot fee — I will set up your portal within 48 hours of payment.”

**Pros:** Professional, receipt automatic, familiar to businesses  
**Cons:** Requires Stripe account verification for live payouts

---

## Option 2 — Simple invoice (Zelle / Venmo / PayPal / wire)

For friends or local businesses who prefer informal payment:

1. Send one-page pilot summary from [REAL_ESTATE_PILOT_OFFER.md](REAL_ESTATE_PILOT_OFFER.md) or [PILOT_OFFER.md](PILOT_OFFER.md)
2. Email text:

> Pilot fee: **USD 99** one-time for 30 days, setup included.  
> Pay via [Zelle / Venmo / PayPal] to [your handle].  
> Reply with payment confirmation and I will send your intake link setup call link.

3. Save screenshot or email confirmation for your records

**Pros:** Same-day, zero integration  
**Cons:** Less professional; harder to scale

---

## Option 3 — PayPal / Square invoice

Same as Option 2 but use PayPal “Request money” or Square invoice for a PDF receipt.

---

## Option 4 — In-app Stripe Checkout (after engineering drill)

When you are ready for self-serve **USD 49 / USD 99** monthly upgrades on the live site:

```bash
export STRIPE_SECRET_KEY=sk_test_...          # never commit
export STRIPE_WEBHOOK_SECRET=whsec_...
export STRIPE_PRO_PRICE_ID=price_...
export STRIPE_BUSINESS_PRICE_ID=price_...
./scripts/drills/setup-stripe-sandbox.sh
./scripts/drills/stripe-checkout-drill.sh
./scripts/drills/record-drill.sh stripe-checkout pass \
  --env "production us-east-1" \
  --operator "your-name" \
  --notes "Checkout 4242 upgraded plan to PRO"
```

Until this drill passes, tell customers: **“I will invoice the pilot; monthly billing inside the app is coming next.”**

---

## What to send after payment

| Step | Action |
|---|---|
| 1 | Thank-you email with kickoff Calendly or proposed times |
| 2 | Confirm organization slug and business name |
| 3 | Schedule 30-minute setup call |
| 4 | After setup, send: intake URL, dashboard link, 2-minute “how to check inbox daily” |
| 5 | Calendar invite for day-14 check-in |

**Intake URL shape:**

```text
https://request-flow-ai-steel.vercel.app/public-request.html?organization=THEIR-SLUG
```

(Replace with their slug after onboarding creates the organization.)

---

## Simple pilot agreement (email — not legal advice)

Copy and adapt:

> **RequestFlowAI 30-day pilot**  
> Fee: USD [0 / 99 / 299] — paid [date/method]  
> Includes: intake link, dashboard, rule-based triage, founder email support  
> Excludes: CRM integration, custom development, AI summaries  
> You agree to: route real requests through the portal, check inbox on business days, 15-min feedback call at end  
> Either party can stop after 30 days. No auto-renewal unless we agree in writing on USD 49/month Pro.

---

## Pricing cheat sheet (customer-facing)

| Offer | Price | When to use |
|---|---|---|
| Feedback pilot | USD 0 | First realtor / friend; testimonial in writing |
| Commitment pilot | USD 99 one-time | Default paid pilot |
| Setup + pilot | USD 299 one-time | They want you to do everything |
| Pro monthly | USD 49/month | After successful pilot (site pricing) |
| Business monthly | USD 99/month | Team with higher volume |

Finance planning docs use USD 29 / 79 / 199 — **the live site shows USD 49 / 99**. Quote customers
what the **website** shows unless you negotiate a written pilot discount.

---

## Red lines

- Do not share Stripe secret keys or webhook secrets in chat, email, or git
- Do not mark `stripe-checkout` drill pass until Checkout + webhook actually updated the plan
- Do not promise refunds you cannot honor — keep pilot terms simple

First dollar beats perfect billing infrastructure. Collect manually, then automate.
