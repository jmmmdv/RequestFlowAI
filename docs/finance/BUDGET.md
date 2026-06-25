# RequestFlowAI startup budget

RequestFlowAI is developed as a lean SaaS product. The goal is to keep monthly costs low before
revenue, validate customer demand, and avoid unnecessary spending.

## Current budget stage

**Stage:** Pre-revenue / MVP validation

The main goal is not scale yet. The main goal is to prove that real customers want RequestFlowAI.

## Monthly cost targets

| Category | Tool / Service | Target Monthly Cost | Notes |
|---|---|---|---|
| Frontend Hosting | Vercel | USD 0 - 20 | Start with free/pro tier only if needed |
| Backend / API | AWS | USD 0 - 25 | Keep usage low during MVP |
| Database | DynamoDB / AWS | USD 0 - 10 | Use free tier where possible |
| AI Analysis | OpenAI API | USD 5 - 50 | Strict spending limit required |
| Email / Notifications | Resend or similar | USD 0 - 20 | Use free tier first |
| Payments | Stripe | USD 0 monthly | Fees only when revenue starts |
| Domain | Already owned / Namecheap | Paid yearly | Not monthly-critical |
| Monitoring / Logs | Free tools first | USD 0 - 10 | Avoid expensive monitoring early |

**Note:** The production-shaped stack in this repository today uses PostgreSQL on RDS (not DynamoDB).
Treat the database row as a budget category; map actual spend to whatever datastore is live in your
AWS account and keep the same USD 0 - 10 MVP target where possible.

## Target monthly budget

### Safe early target

**USD 25 - 100 per month**

### Maximum before real revenue

**USD 150 per month**

Do not exceed this unless there is clear customer validation (signed pilot, LOI, or first payment).

## OpenAI API cost control rules

1. Set a **hard monthly spend cap** in the OpenAI dashboard (start at **USD 25**, raise only with
   paying customers).
2. **No unlimited AI analysis** on any plan — every tier gets a monthly analysis quota.
3. **Cache or persist** analysis results; do not re-call the model for the same request on every
   page view.
4. **Fallback to rule-based triage** when quota or budget is exhausted (already supported in the
   MVP foundation).
5. **Log token usage per organization** before enabling LLM in production (extension path).
6. Review OpenAI invoice weekly during pilots.

## Decision rule before spending money

RequestFlowAI should stay financially small until customers prove demand.

Before spending more money, ask:

1. Does this help us get customers?
2. Does this reduce real operational pain?
3. Does this increase revenue potential?
4. Can we test this cheaper first?

If the answer is no to all four, do not spend yet.

## Current CFO decision

RequestFlowAI should stay in a lean MVP budget until the first paying customer or strong pilot
customer feedback. Hosted pilot infrastructure may temporarily push AWS above the table targets;
treat that as a **time-boxed validation expense**, not a new baseline, unless revenue follows within
60 days.
