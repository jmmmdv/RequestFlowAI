# 14-day revenue sprint

Founder action plan to earn the **first dollars** from RequestFlowAI without waiting for enterprise
features or paid AI. Honest about what is live today.

**Live today (verified):**

- Frontend: https://request-flow-ai-steel.vercel.app
- API: App Runner production (`actuator/health` UP)
- Cognito sign-in: production drill passed
- Rule-based intake, dashboard, public portal: working

**Not live yet for self-serve billing:**

- In-app Stripe Checkout on production (stack has empty Stripe parameters; drill `stripe-checkout` pending)
- Paid OpenAI analysis (intentionally disabled)

**Implication:** Collect the first pilot payments **manually** (invoice, Stripe Payment Link, Zelle,
Venmo, PayPal) while you run pilots. Turn on in-app checkout in week 2 if you complete the Stripe
sandbox drill.

**Related:** [FIRST_PAYMENT_PLAYBOOK.md](FIRST_PAYMENT_PLAYBOOK.md) ·
[REAL_ESTATE_OUTREACH_READY.md](REAL_ESTATE_OUTREACH_READY.md) ·
[PILOT_OFFER.md](PILOT_OFFER.md)

---

## Revenue paths ranked by speed

| Path | Time to first USD | Effort | Best for |
|---|---|---|---|
| **A — Warm intro pilot (USD 0–99)** | 3–7 days | Low | Realtor friend, agency owner you know |
| **B — “Portal in 48 hours” setup** | 7–14 days | Medium | USD 299–499 one-time + optional monthly |
| **C — In-app Pro/Business (USD 49–99/mo)** | 14+ days | Higher | After Stripe drill passes on production |
| **D — Agency white-label angle** | 30+ days | Higher | After one case study exists |

**Start with A.** You already have product + hosting. You need conversations and one paid yes.

---

## Your offer (keep it simple)

### Default pilot (first 3 customers)

| Item | Terms |
|---|---|
| Duration | 30 days |
| Setup | USD 0 |
| Price | **USD 0** (feedback) or **USD 99 one-time** (commitment) |
| Deliverable | Public intake link + dashboard + founder setup call |
| Not included | CRM replacement, MLS, AI summaries, custom dev |

### If they want more hand-holding

| Item | Terms |
|---|---|
| Name | Request portal setup (48-hour kickoff) |
| Price | **USD 299 one-time** (solo agent) or **USD 499** (small team) |
| Includes | Intake link on site/signature, 3 sample workflows, 2 check-in calls |
| Monthly after | USD 49/month Pro when they love it (manual invoice until Stripe live) |

Do not discount below USD 99 without strong testimonial commitment in writing.

---

## Day-by-day plan

### Days 1–2 — Prepare and warm network

- [ ] Read [REAL_ESTATE_DEMO_SCRIPT.md](REAL_ESTATE_DEMO_SCRIPT.md) (15 min)
- [ ] Open live demo: https://request-flow-ai-steel.vercel.app
- [ ] Submit 2 sample requests from [REAL_ESTATE_SAMPLE_REQUESTS.md](REAL_ESTATE_SAMPLE_REQUESTS.md)
- [ ] Copy [VALIDATION_TRACKER_TEMPLATE.md](VALIDATION_TRACKER_TEMPLATE.md) into a spreadsheet
- [ ] List **10 warm contacts** (realtor friend + 9 owners: HVAC, tutoring, PM, agency, etc.)
- [ ] Send **3 messages today** using [REAL_ESTATE_OUTREACH_READY.md](REAL_ESTATE_OUTREACH_READY.md) or [CUSTOMER_OUTREACH_MESSAGES.md](CUSTOMER_OUTREACH_MESSAGES.md)

**Goal:** 3 conversations booked.

### Days 3–5 — Discovery calls (15–20 min each)

- [ ] Use [REAL_ESTATE_DISCOVERY_QUESTIONS.md](REAL_ESTATE_DISCOVERY_QUESTIONS.md) or [CUSTOMER_DISCOVERY_QUESTIONS.md](CUSTOMER_DISCOVERY_QUESTIONS.md)
- [ ] Ask: volume, tools, missed follow-ups, willingness to try 30 days
- [ ] Log every call in validation tracker
- [ ] Book **1 demo** for anyone who says “maybe”

**Goal:** 5 conversations total; 1 demo scheduled.

### Days 6–8 — Demos and pilot closes

- [ ] Run demo script; submit a live request on the call
- [ ] Close with: “30-day pilot, free or USD 99 — which feels fair if I set you up this week?”
- [ ] Send [REAL_ESTATE_PILOT_OFFER.md](REAL_ESTATE_PILOT_OFFER.md) (or generic [PILOT_OFFER.md](PILOT_OFFER.md)) by email after the call
- [ ] Collect **USD 99** via [FIRST_PAYMENT_PLAYBOOK.md](FIRST_PAYMENT_PLAYBOOK.md) before or on kickoff

**Goal:** **1 pilot started** (paid or free with written feedback agreement).

### Days 9–11 — Deliver and prove value

- [ ] Kickoff call: share intake link, show inbox, agree who checks daily
- [ ] Help them place link (website, email signature, open house QR)
- [ ] Check they received at least **3 real requests** in the portal
- [ ] Send 5 more outreach messages to fill pipeline

**Goal:** Pilot customer actively using the product.

### Days 12–14 — Second pilot + payment infrastructure

- [ ] 15-minute check-in with pilot #1 — capture quotes and friction
- [ ] Close **pilot #2** (aim for paid USD 99)
- [ ] Run `./scripts/drills/setup-stripe-sandbox.sh` when you have Stripe test keys (enables in-app checkout path)
- [ ] Record drill: `./scripts/drills/record-drill.sh stripe-checkout pass ...`

**Goal:** **USD 99–198 collected** OR 2 active pilots with clear path to paid conversion.

---

## Who to contact first (suggested order)

1. **Realtor / real estate friend** — you have a tailored pack; fast feedback
2. **Home services** (HVAC, plumber, handyman) — urgent vs quote pain
3. **Tutoring / lessons** — seasonal message spikes
4. **Small marketing / web agency** — client change requests everywhere
5. **Property manager** (under 50 units) — maintenance request chaos
6. **Limo / car service** — booking texts
7. **Bookkeeper / solo CPA** — client document requests
8. **Cleaning / landscaping** — owner in the field
9. **IT freelancer with small business clients** — reseller angle later
10. **Former colleague with ops pain** — warm trust

---

## What to say when they ask “Is it finished?”

> It is a focused MVP: one intake link, one inbox, priority suggestions, and status tracking. It is
> not Salesforce. We are running short pilots with real businesses to prove it saves time before we
> add more automation. You get founder support directly.

---

## Success metrics (14 days)

| Metric | Minimum target | Strong target |
|---|---|---|
| Outreach messages sent | 15 | 25 |
| Discovery conversations | 5 | 10 |
| Demos delivered | 2 | 4 |
| Pilots started | 1 | 2 |
| Cash collected | USD 0 + testimonial promise | USD 99–299 |
| Real requests in pilot portal | 10+ | 30+ |

---

## What NOT to do this sprint

- Do not enable paid OpenAI (burns margin; not validated)
- Do not build custom features for free
- Do not claim “thousands of customers” or live production Stripe if checkout drill is not pass
- Do not spend on ads before 2 manual pilots complete
- Do not rewrite the app — sell what works today

---

## After the first USD 99

1. Ask for a **short testimonial** and permission to use logo/name
2. Offer **USD 49/month** Pro to continue (manual invoice month 2, or in-app after Stripe)
3. Use their story in [PILOT-OUTREACH.md](../portfolio/PILOT-OUTREACH.md) (update live demo URL)
4. Raise setup fee to USD 299 for customer #4+

---

## Weekly founder checklist (repeat)

| Done? | Action |
|---|---|
| ☐ | 5+ outreach touches |
| ☐ | Validation tracker updated |
| ☐ | 1 demo or discovery call |
| ☐ | Pilot customer inbox checked with them |
| ☐ | No custom code promises |

Money follows **conversations → pilot → payment**. Run the calendar above before building more product.
