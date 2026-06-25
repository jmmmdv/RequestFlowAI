# AI cost control — implementation summary

RequestFlow AI built a **cost-control foundation before any paid AI integration**. This document
summarizes what is implemented today, what is intentionally not enabled, and what must happen before
OpenAI or another paid provider is turned on.

**Related docs:** [API_COST_GUARDRAILS.md](API_COST_GUARDRAILS.md) ·
[ADR-001-AI-COST-CONTROL.md](../architecture/ADR-001-AI-COST-CONTROL.md) ·
[FINANCE_CHECKLIST.md](../finance/FINANCE_CHECKLIST.md)

---

## 1. Overview

RequestFlow AI will eventually use paid LLM analysis for public request triage. Variable API spend
can exceed a lean MVP budget quickly if integration precedes guardrails. The team implemented **seven
documented steps** — budget caps, usage persistence, quota limits, a single analysis facade, a
disabled provider seam, and an ADR — so that **paid AI can be added in one controlled place** without
rewriting intake, billing, or tenant isolation.

**No OpenAI SDK, API keys, or paid API calls exist in the repository today.**

---

## 2. Completed steps

| Step | Name | What was delivered |
|---|---|---|
| **1** | AI budget guardrail service | `AiBudgetProperties`, `AiBudgetService`, `AiBudgetStatus` — monthly cap (USD 50 default), 70% warning, 90% hard stop, `canUsePaidAi()` |
| **2** | AI usage event persistence | Flyway `V9__add_ai_usage_events.sql`, `AiUsageEvent` entity, `AiUsageEventRepository`, `AiUsageEventService` |
| **3** | Zero-cost usage tracking for rule-based intake | `RequestIntakeService` records one `PUBLIC_INTAKE_CLASSIFICATION` event per new submission; idempotent replay skips recording |
| **4** | Monthly AI analysis quota service | `Plan.monthlyAiAnalysisLimit()` (FREE 25 · PRO 1,000 · BUSINESS 10,000), `AiAnalysisQuotaService` — count, status, `assertAiAnalysisCapacity()` (not enforced on intake yet) |
| **5** | `RequestAnalysisService` facade | Single entry point for public analysis; `RequestAnalysisInput` / `RequestAnalysisResult`; intake no longer calls classifier directly |
| **6** | Disabled AI provider interface | `AiRequestAnalysisProvider`, `DisabledAiRequestAnalysisProvider`, `requestflow.ai.provider.enabled=false` — always returns empty, falls back to rules |
| **7** | AI cost-control ADR | [ADR-001](../architecture/ADR-001-AI-COST-CONTROL.md) — policy, preconditions, CFO principle |

Detailed step notes live in [API_COST_GUARDRAILS.md](API_COST_GUARDRAILS.md#implementation-status).

---

## 3. Current state

| Topic | Status |
|---|---|
| OpenAI SDK | **Not added** |
| API keys in repo or config | **Not added** |
| Paid AI API calls | **Not made** |
| Request analysis path | **Rule-based** (`RuleBasedRequestClassifier` via `RequestAnalysisService`) |
| Usage per new intake | **One zero-cost** `ai_usage_event` (`NOT_PAID_AI`, `paidAiUsed=false`) |
| Idempotent intake replay | **Does not** create a second usage event |
| Paid AI provider | **Disabled** (`requestflow.ai.provider.enabled=false`) |
| Per-plan AI quota on intake | **Not enforced** yet (service ready) |
| Global budget on intake | **Not wired** to live spend (service ready) |

Public intake API responses, classification fields, and idempotency behavior are **unchanged** from
the pre-guardrail rule-based MVP.

---

## 4. Main files

### Documentation

| File | Role |
|---|---|
| [API_COST_GUARDRAILS.md](API_COST_GUARDRAILS.md) | Operational rules, spend pipeline, step-by-step implementation status |
| [ADR-001-AI-COST-CONTROL.md](../architecture/ADR-001-AI-COST-CONTROL.md) | Accepted decision: no paid AI until preconditions met |

### Application — analysis facade and provider

| File | Role |
|---|---|
| `src/main/java/.../intake/RequestAnalysisService.java` | Facade: provider first, then rule-based fallback |
| `src/main/java/.../ai/provider/AiRequestAnalysisProvider.java` | Paid AI abstraction (`Optional` result) |
| `src/main/java/.../ai/provider/DisabledAiRequestAnalysisProvider.java` | Default no-op provider |
| `src/main/java/.../intake/RuleBasedRequestClassifier.java` | Active classification engine |
| `src/main/java/.../intake/RequestIntakeService.java` | Intake + one zero-cost usage record per new submission |

### Application — budget, usage, quota

| File | Role |
|---|---|
| `src/main/java/.../ai/budget/AiBudgetService.java` | Global monthly budget math and status |
| `src/main/java/.../ai/budget/AiBudgetProperties.java` | `requestflow.ai.budget.*` configuration |
| `src/main/java/.../ai/usage/AiUsageEventService.java` | Record events, monthly cost rollups |
| `src/main/java/.../ai/usage/AiUsageEvent.java` | JPA entity for `ai_usage_event` |
| `src/main/java/.../ai/usage/AiAnalysisQuotaService.java` | Per-tenant monthly analysis limits |

### Database migrations

| File | Role |
|---|---|
| `src/main/resources/db/migration/V9__add_ai_usage_events.sql` | Creates `ai_usage_event` table |
| `src/main/resources/db/migration/V10__extend_ai_usage_event_enums.sql` | Adds `PUBLIC_INTAKE_CLASSIFICATION`, `NOT_PAID_AI` |

### Configuration (non-secret defaults)

```properties
requestflow.ai.budget.enabled=true
requestflow.ai.budget.monthly-budget-usd=50.00
requestflow.ai.provider.enabled=false
```

---

## 5. What must happen before enabling OpenAI

Do **not** set `requestflow.ai.provider.enabled=true` or deploy a live LLM provider until **all** of
the following are complete and tested:

1. **OpenAI (or chosen) provider implemented server-side only** — implements `AiRequestAnalysisProvider`; no browser calls
2. **API key stored only in environment or secret manager** — never committed to git
3. **Budget hard stop tested** — `AiBudgetService` blocks new paid calls at 90% of monthly cap
4. **Per-plan quota enforcement tested** — `AiAnalysisQuotaService.assertAiAnalysisCapacity()` called before paid analysis
5. **Usage recorded for every paid AI attempt** — success, failure, and fallback paths write `ai_usage_event`
6. **Fallback to rule-based classifier tested** — intake still succeeds when provider/budget/quota fails
7. **No uncontrolled retry loops** — bounded or single-attempt calls; no runaway spend on errors
8. **Production warning / hard-stop logging or alerting** — operators see 70% warning and 90% hard stop

Full checklist: [ADR-001 preconditions](../architecture/ADR-001-AI-COST-CONTROL.md#preconditions-before-enabling-paid-ai).

---

## 6. CFO summary

RequestFlow AI is now **prepared for paid AI integration** — budget service, usage ledger, plan
quotas, analysis facade, and disabled provider seam are in place and tested.

**Paid AI should remain disabled** until customer demand, pricing, and gross margin are validated.
Until then, rule-based analysis with zero-cost usage tracking is the honest, financially safe product
path.
