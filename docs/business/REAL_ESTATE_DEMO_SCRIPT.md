# Real estate demo script

Step-by-step guide for showing RequestFlowAI to a realtor or small real estate team. Tone:
professional, honest, not pushy.

**Live demo:** https://request-flow-ai-steel.vercel.app

**Related:** [REAL_ESTATE_SAMPLE_REQUESTS.md](REAL_ESTATE_SAMPLE_REQUESTS.md) ·
[REAL_ESTATE_PILOT_OFFER.md](REAL_ESTATE_PILOT_OFFER.md)

---

## Before the demo (5 minutes)

- [ ] Confirm demo mode vs production sign-in (use production if Cognito is configured for your guest)
- [ ] Prepare one public intake link with their organization slug (or `local` for generic demo)
- [ ] Open [REAL_ESTATE_SAMPLE_REQUESTS.md](REAL_ESTATE_SAMPLE_REQUESTS.md) — pick 2–3 examples to submit live
- [ ] Have dashboard inbox visible on a second tab or screen share ready

---

## 30-second intro

> Thanks for making time. RequestFlowAI is a simple request inbox for small service teams. For real
> estate, it helps you collect buyer and seller inquiries in one place, see what is urgent, and
> follow through without digging through email, text, and voicemail.
>
> This is an early product — not a full CRM. I want to show you how intake and triage work today,
> hear whether it matches your workflow, and see if a short pilot would be useful. No pressure either way.

---

## Problem statement (1–2 minutes)

> Most agents I talk to do not lack effort — they lack one clear queue. Leads arrive on the
> website, through Zillow, by text, and from past clients. Urgent seller questions sit next to
> casual browsers. Showings get requested on Saturday while you are in an open house.
>
> The cost is not always one lost deal — it is mental load, slow replies, and the stress of not
> knowing what you missed.

**Pause:** “Does any of that sound familiar? Where do things land for you today?”

---

## Step 1 — Public intake form (3–4 minutes)

1. Open the public request page:  
   `https://request-flow-ai-steel.vercel.app/public-request.html?organization=YOUR-SLUG`
2. Explain: “This is what a buyer or seller would see — one link on your site, email signature, or QR code.”
3. Submit a live example (buyer lead in McLean or seller valuation — see sample requests doc).
4. Point out fields: name, email, title, details, optional category/urgency if exposed.

**Say:**

> Every submission gets a reference number. The client knows you received it. Your team sees it in
> one inbox — not scattered across personal email.

**Ask:** “Would your clients use a form like this, or do they insist on text and phone only?”

---

## Step 2 — Request appears in the dashboard (3–4 minutes)

1. Switch to the dashboard workspace (`#workspace` or scroll to inbox).
2. Refresh or wait for the new request to appear.
3. Show: title, requester, category, suggested priority, internal summary, recommended next action.
4. Open status controls — move from backlog to in progress.

**Say:**

> You are not replacing your CRM overnight. You are giving every inbound request a home and a status
> so nothing disappears when your phone blows up during a showing.

**Ask:** “Who on your team would own this inbox — you, admin, or rotating duty?”

---

## Step 3 — Priority, category, and next action (2–3 minutes)

1. Compare two sample requests side by side (urgent seller vs low-priority relocation question).
2. Explain rule-based triage:

**Say:**

> Today the system uses clear rules — keywords and urgency — to suggest category and priority. It is
> not ChatGPT. It is fast, predictable, and good enough to sort a Saturday morning inbox.
>
> “Urgent” and “today” bump priority. Words like “valuation” or “buyer” help route sales-style leads.
> You always stay in control — this is a suggestion, not autopilot.

**Ask:** “If you opened this on Monday morning, which request would you want at the top?”

---

## Step 4 — Reducing missed follow-ups (2 minutes)

**Say:**

> The goal is not more software — it is fewer “I never heard back” moments. One link, one queue,
> visible status, and a suggested next step mean your admin or partner can pick up where you left off.
>
> Reference numbers help when a client calls: “Yes, I have request RF-… in progress.”

**Ask:** “What is your biggest follow-up leak today — buyer leads, seller valuations, or showings?”

---

## What is rule-based today

Be explicit:

| Capability | Today |
|---|---|
| Category suggestion | Keyword rules (sales, support, billing, change request, general) |
| Priority suggestion | Urgency words (urgent, today, no rush, etc.) |
| Internal summary | Short excerpt of title + message |
| Recommended next action | Template by category + priority |
| AI / OpenAI | **Disabled** — no per-request LLM cost in pilot |

---

## What AI may improve later (if validated)

Only discuss as a future path after pilots confirm demand:

- Richer summaries of long buyer emails
- Smarter routing by neighborhood, price band, or buyer vs seller intent
- Draft reply suggestions for admin review (human always sends)
- Trend notes (“five valuation requests this week in one zip code”)

**Say:**

> We are not selling AI today. If rule-based triage helps and you ask for smarter summaries, that
> becomes a paid upgrade once pricing and margins work.

---

## Questions to ask during the demo

1. Would you put this link on your website, or only use it internally?
2. What is missing compared to how you work today?
3. Does the suggested priority match your gut on these examples?
4. Would your broker or franchise allow a tool like this?
5. What would make you check this inbox every morning?

---

## Close with a 30-day pilot invitation (2 minutes)

**Say:**

> If this feels directionally useful, I would suggest a 30-day pilot — not a year-long contract.
> We set up your intake link, you route real or test leads through it, and we do two short check-ins.
> For the first real estate pilot, I am recommending free or USD 99 with the understanding you give
> honest feedback on what works and what does not.
>
> Success for me is learning whether this saves you time — not convincing you to ditch your CRM.

Hand them [REAL_ESTATE_PILOT_OFFER.md](REAL_ESTATE_PILOT_OFFER.md) or summarize verbally.

**Next steps if interested:**

1. Agree on pilot start date and who participates
2. Choose organization slug and share intake link placement plan
3. Schedule 15-minute kickoff and two-week check-in
4. Log session notes in your validation tracker

**Next steps if not interested:**

> Thank them for candor. Ask what they would use instead and whether you may follow up after the
> product improves.

---

## Demo do nots

- Do not claim MLS, CMA, or marketing automation integration
- Do not promise AI features that are not live
- Do not criticize their current CRM — complement or sit beside it
- Do not push paid tiers during the first conversation
