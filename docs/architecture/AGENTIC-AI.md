# Agentic AI: from demo to dependable system

The current `PlanningAgent` demonstrates the mechanics without an external model:

1. **Observe:** validate and normalize a goal.
2. **Reason:** classify it with deterministic policy.
3. **Plan:** decompose it into discover, implement, verify.
4. **Act:** invoke the repository as a tool—or perform a dry run.
5. **Report:** return classification, trace, and created IDs.

This is agentic because software selects and executes actions toward a goal. It is intentionally not
presented as magical intelligence: the policy is narrow, inspectable, and thoroughly testable.

## LLM extension seam

Introduce a `GoalPlanner` interface. Keep the current rules implementation as fallback and add an
LLM-backed implementation that returns a schema-validated plan. Do not give a model direct database
or shell access; expose allow-listed, typed tools through an orchestrator that enforces:

- maximum steps, elapsed time, and cost;
- input/output schema validation and content limits;
- authorization in the tool layer, not the prompt;
- human approval for deletion, deployment, messages, or expensive actions;
- idempotency keys, transaction boundaries, and compensation;
- correlation IDs and audit events without secrets or hidden chain-of-thought;
- model/prompt versioning, evaluation datasets, and rollback.

Evaluation set: 20 representative goals plus adversarial input. Score classification, plan relevance,
unsafe-action rate, duplicate-action rate, latency, cost, and human acceptance. CI should block a model
or prompt change when safety or task success regresses beyond an agreed threshold.
