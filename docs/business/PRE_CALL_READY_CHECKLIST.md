# Pre-call ready / not ready checklist

Read this **before every customer call, demo, or pilot close**. One page. Honest positioning only.

**Live demo:** https://request-flow-ai-steel.vercel.app

---

## Quick verdict

| If the customer expects… | You are ready? |
|---|---|
| 20-minute demo + 30-day pilot with founder help | **Yes** |
| USD 99 paid pilot (manual payment) | **Yes** |
| Self-serve signup + pay USD 49/month in the app | **Not yet** |
| Full CRM / MLS / marketing automation replacement | **No** |
| AI-written replies and smart routing | **No** (rule-based only today) |

---

## Ready — you can say yes

- [ ] **Live site loads** — https://request-flow-ai-steel.vercel.app
- [ ] **Sign-in works** — Cognito login (not demo mode) if showing production workspace
- [ ] **Public intake works** — submit a test request; it appears in inbox
- [ ] **Dashboard shows** — category, priority, summary, next action, status
- [ ] **You can explain the product in one sentence:**  
  *“One link for client requests, one inbox, know what’s urgent, track follow-up.”*
- [ ] **Pilot offer is clear** — 30 days, USD 0 or USD 99, founder setup included
- [ ] **Payment path chosen** — Stripe Payment Link, Zelle, or Venmo ([FIRST_PAYMENT_PLAYBOOK.md](FIRST_PAYMENT_PLAYBOOK.md))
- [ ] **You will check in twice** during the pilot (you, not “the platform”)

---

## Not ready — do not promise

- [ ] **In-app Upgrade / Stripe Checkout on production** — drill `stripe-checkout` still pending unless you verified otherwise today
- [ ] **Automatic billing after pilot** — say “manual invoice or in-app billing coming soon”
- [ ] **Email / SMS / WhatsApp ingestion** — not in MVP
- [ ] **Two-way CRM sync** (Follow Up Boss, kvCORE, Salesforce, etc.)
- [ ] **OpenAI / LLM summaries** — disabled until demand and margin are validated
- [ ] **White-label mobile app, per-agent branding, franchise compliance pack**
- [ ] **24/7 support, SLA, or “enterprise security certified”**
- [ ] **Thousands of customers / production payments at scale** — not true; say “early pilot”

---

## 30-second honest intro (copy-ready)

> RequestFlowAI is an early request inbox for small teams. Clients submit through one link; you see
> everything in a dashboard with suggested priority and next steps. It is not a full CRM. We run
> 30-day pilots with founder support while we prove it saves time before we add more automation.

---

## Before you share screen

1. Open dashboard in one tab; public intake form in another  
2. Submit one fresh test request (or use [REAL_ESTATE_SAMPLE_REQUESTS.md](REAL_ESTATE_SAMPLE_REQUESTS.md))  
3. Confirm header says **Sign in** / **Authenticated** — not stuck in unintended demo mode  
4. Have [REAL_ESTATE_DEMO_SCRIPT.md](REAL_ESTATE_DEMO_SCRIPT.md) or discovery questions open  
5. Know their industry pain (buyer leads, showings, missed email, etc.)

---

## Close the call — safe asks

| Outcome | Say this |
|---|---|
| **Interested** | “30-day pilot — USD 0 with feedback, or USD 99 one-time. I’ll send setup times and payment link.” |
| **Maybe** | “Can I follow up in 48 hours? One recent missed lead example would help me prep.” |
| **No** | “What do you use instead? That helps me even if this isn’t a fit.” |

---

## After the call (5 minutes)

- [ ] Log row in [VALIDATION_TRACKER_TEMPLATE.md](VALIDATION_TRACKER_TEMPLATE.md)  
- [ ] Send pilot offer email if yes/maybe  
- [ ] Schedule kickoff if paid or committed free pilot  
- [ ] Do **not** promise custom features unless they are paying USD 99+

---

## Red flags — pause the sale

Stop or reframe if you hear yourself saying:

- “It integrates with your CRM out of the box”
- “AI will handle replies for you”
- “Just click Upgrade and you’re on Pro” *(unless Stripe drill passed today)*
- “We have hundreds of customers”
- “I’ll build that custom feature this week for free”

---

## Emergency lines if something breaks on the call

| Problem | What to say |
|---|---|
| Sign-in error | “Let me use demo intake on the public form — the flow is the same; I’ll fix auth after this call.” |
| Request didn’t appear | Refresh; submit again; show a prepared screenshot if needed |
| They want AI | “Today is rule-based triage — fast and predictable. AI is on the roadmap after pilots confirm demand.” |

---

## Related docs

- [REVENUE_SPRINT_14_DAYS.md](REVENUE_SPRINT_14_DAYS.md)
- [REAL_ESTATE_OUTREACH_READY.md](REAL_ESTATE_OUTREACH_READY.md)
- [REAL_ESTATE_PILOT_OFFER.md](REAL_ESTATE_PILOT_OFFER.md)
- [PRODUCTION-PILOT-CHECKLIST.md](../saas/PRODUCTION-PILOT-CHECKLIST.md) — technical go/no-go

**You are ready for the conversation. You are not ready to oversell.**
