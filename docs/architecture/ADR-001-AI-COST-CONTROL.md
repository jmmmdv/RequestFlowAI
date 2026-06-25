# ADR-001: AI Cost Control Before Paid AI Integration

## Status

**Accepted** — 2026-06-25

## Context

RequestFlow AI will eventually use paid AI and LLM providers (for example OpenAI) to enrich public
request analysis — classification, priority, summaries, and recommended next actions beyond what
keyword rules can infer.

Paid AI introduces **variable and unpredictable cost**. A single misconfigured integration, retry
loop, or unbounded tenant can consume more API spend in days than the entire lean MVP infrastructure
budget for a month. That risk is unacceptable while the product is still in **lean MVP and customer
validation** — before pricing, gross margin, and repeat usage are proven with real pilots.

The codebase therefore ships a **production-minded cost-control foundation** without calling any paid
API today:

| Layer | Purpose | Current state |
|---|---|---|
| `AiBudgetService` | Global monthly spend cap, warning, hard stop | Implemented |
| `AiUsageEventService` | Persist analysis metadata and estimated cost | Implemented |
| `AiAnalysisQuotaService` | Per-plan monthly AI analysis limits | Implemented (not enforced on intake) |
| `RequestAnalysisService` | Single facade for public request analysis | Implemented |
| `AiRequestAnalysisProvider` | Paid AI abstraction | Disabled default (`requestflow.ai.provider.enabled=false`) |
| `RuleBasedRequestClassifier` | Deterministic triage | **Active path** — zero paid AI cost |

Public intake records **zero-cost** `ai_usage_event` rows for rule-based classification so usage,
quotas, and future CFO reporting have real data before any LLM is switched on.

## Decision

1. **OpenAI / paid AI integration remains disabled by default.**  
   `requestflow.ai.provider.enabled=false` and `DisabledAiRequestAnalysisProvider` return no paid
   result. No OpenAI SDK, API keys, or external model calls are added until preconditions below are met.

2. **All public request analysis must go through `RequestAnalysisService`.**  
   `RequestIntakeService` and any future intake paths must not call `RuleBasedRequestClassifier` or an
   LLM client directly.

3. **Future paid AI must implement `AiRequestAnalysisProvider` and be invoked only from
   `RequestAnalysisService`.**  
   The facade tries the provider first; on empty, failure, or guardrail block, it falls back to
   rules.

4. **Paid AI must be protected by the existing guardrail stack:**
   - `AiBudgetService` — global monthly budget, 70% warning, 90% hard stop
   - `AiAnalysisQuotaService` — per-tenant plan limits (FREE 25 · PRO 1,000 · BUSINESS 10,000 / month)
   - `AiUsageEventService` — record every analysis attempt (paid, rule-based, or fallback)

5. **`RuleBasedRequestClassifier` remains the mandatory fallback path.**  
   Budget exhaustion, quota exceeded, provider errors, and disabled provider all degrade to
   rule-based output. Public intake must not fail solely because paid AI is unavailable.

6. **API keys and secrets must never be committed or exposed.**  
   Keys live only in environment variables or a secret manager (for example AWS Secrets Manager on App
   Runner). No keys in `application.properties` in git, frontend code, logs, or documentation.

## Consequences

### Positive

- **USD 0 paid AI cost risk today** — no variable LLM bill while validating product demand
- **Safer MVP and pilot phase** — founders can demo and onboard without surprise API invoices
- **Predictable budget** — monthly cap and per-plan quotas are defined before spend starts
- **Easier future integration** — one facade, one provider interface, persisted usage events
- **Better audit trail** — `ai_usage_event` supports per-tenant and global rollups for CFO review

### Tradeoffs

- **Current analysis is less intelligent than real LLM analysis** — keyword rules miss nuance and
  context that a model could capture
- **Some advanced automation is deferred** — richer summaries, multi-label routing, and semantic
  priority are roadmap items, not live claims
- **Future provider implementation still required** — guardrails alone do not deliver LLM value;
  engineering work remains to add a server-side provider safely

## Preconditions before enabling paid AI

All items must be true before setting `requestflow.ai.provider.enabled=true` or deploying a live
provider in production:

- [ ] **OpenAI (or chosen) provider implemented server-side only** — no browser or Vercel calls
- [ ] **API key stored only in environment / secret manager** — never in the repository
- [ ] **Monthly budget configured** — `AiBudgetService` + provider dashboard hard limit aligned
- [ ] **Per-plan quota enforced** — `AiAnalysisQuotaService.assertAiAnalysisCapacity()` wired before paid calls
- [ ] **Usage events recorded for every paid AI attempt** — including failures and fallbacks
- [ ] **Fallback tested** — budget/quota/provider failure returns same intake success with rule-based output
- [ ] **Tests for budget hard stop** — 90% cap blocks new paid calls
- [ ] **No retry loops that can create runaway spend** — single attempt or bounded retry with cap
- [ ] **Production alert/log for warning and hard-stop thresholds** — `AI_BUDGET_WARNING` / hard stop visible to operators

See also [API_COST_GUARDRAILS.md](../security/API_COST_GUARDRAILS.md) and
[FINANCE_CHECKLIST.md](../finance/FINANCE_CHECKLIST.md).

## CFO principle

**RequestFlowAI should only increase AI/API spending after customer demand, pricing, and gross
margin are validated.**

## Related documentation

- [API cost guardrails](../security/API_COST_GUARDRAILS.md) — implementation steps 1–6
- [Agentic AI extension path](AGENTIC-AI.md) — agent planner seam (separate from intake LLM)
- [Unit economics](../finance/UNIT_ECONOMICS.md) — margin assumptions for paid AI
