# MissionOps AI commercial MVP

## 1. Target customer

The first customer is a small service business with roughly 2–25 employees that receives customer
or internal requests through inconsistent channels and currently organizes them with inboxes,
spreadsheets, chat, or memory.

Good pilot candidates include consulting offices, clinics with non-clinical administrative work,
immigration or document-service offices, small law offices, fleet or limousine operators, and
other local professional-service businesses. The best first pilot has one involved owner or
operations lead, a repeatable request workflow, and enough weekly requests to feel the cost of
missed follow-up without requiring a full CRM.

The initial buyer and primary user are the business owner or operations lead. Employees may submit
requests or complete work, but a sophisticated multi-role team rollout is not required for the
first pilot.

## 2. Primary customer problem

Requests arrive as incomplete emails, calls, messages, notes, or staff questions. Important context
is scattered, ownership is unclear, urgency is judged inconsistently, and follow-up depends on one
person remembering what happened. Generic task boards begin after the request has already been
understood, so they do not solve intake, triage, approval, or traceability.

The first commercial problem to solve is therefore not broad business automation. It is giving an
owner one reliable place to capture a messy request, organize it into actionable work, approve the
proposed response, and see its history.

## 3. Product promise

> “MissionOps AI helps small service businesses turn messy customer or employee requests into
> organized, trackable, AI-assisted work with human approval and audit history.”

For the first MVP, “AI-assisted” means that the product can provide clearly labeled automated
classification, priority, summaries, or recommended next steps. The current implementation is
rule-based and must be described as rule-based or automation-assisted. The product must not claim
to use a real LLM unless a real, evaluated LLM integration is added later.

## 4. First paid pilot offer

Offer a founder-led, manually onboarded pilot to one small service business:

- one business workspace;
- one owner or operations lead as the primary user;
- manual request entry through the MissionOps AI dashboard;
- a request inbox with status, priority, and request history;
- automation-assisted triage and suggested execution work;
- human approval before approved suggestions create or trigger consequential work;
- weekly setup and feedback support;
- a 30-day pilot with success reviewed at the end.

The pilot may be manually invoiced. Self-service Stripe Checkout, upgrades, cancellation, and a
billing portal are not requirements for proving customer value. Pricing should be agreed directly
with the pilot customer and should not imply features or service levels that are not operational.

## 5. MVP demo flow

1. A business owner opens MissionOps AI and sees a request-focused dashboard.
2. The owner pastes a realistic customer, employee, or owner request into a manual intake form.
3. MissionOps AI saves the raw request without losing the original wording.
4. The request appears in a tenant-scoped inbox with its requester, source, status, and received
   time.
5. Automation-assisted triage proposes a summary, category, priority, and suggested next steps.
6. The owner reviews and edits the proposal rather than trusting an opaque automatic decision.
7. A routine proposal can be approved to create linked execution work. Risky or high-impact work
   remains blocked until explicit human approval.
8. The owner updates the request and related work as execution progresses.
9. The request detail view shows what was received, what was suggested, what was approved, who
   acted, and when each change occurred.

The first code slice will implement only steps 2–4: tenant-scoped manual creation and listing. Later
slices will add triage, approval, work creation, and the complete audit timeline.

## 6. Core domain model

### ServiceRequest

Represents raw intake from a customer, employee, or business owner. It is the durable source record
for what was asked before the business interprets or decomposes it.

Initial fields should include:

- identifier;
- trusted tenant identifier;
- requester name;
- requester type (`CUSTOMER`, `EMPLOYEE`, or `BUSINESS_OWNER`);
- source (`MANUAL` for the first slice);
- subject or short label;
- raw request text;
- workflow status;
- received, created, and updated timestamps;
- optimistic-lock version.

Later slices may add requester contact details, category, assigned owner, due date, accepted
priority, and links to recommendations and work items. Contact details introduce additional privacy
obligations and should not be collected before they serve the pilot workflow.

### WorkItem

Represents organized execution work created after triage or approval. It answers what the team will
do, not what the requester originally said. Existing work-item priority and execution status can be
reused as the starting point.

### Recommendation

Represents a versioned, reviewable suggestion associated with a service request: normalized
summary, proposed category, proposed priority, suggested next steps, generation method, and
generation time. Rule-based output must be labeled as automation-assisted. A future LLM-backed
recommendation must also record model and prompt versions, latency, and review outcome without
storing hidden chain-of-thought.

### ApprovalDecision

Represents an attributable human decision to approve, edit, reject, or defer a recommendation or
high-impact action. It records the actor, timestamp, decision, and optional reason. Approval must be
idempotent and authorization must be enforced outside prompts.

### RequestEvent

Represents append-only audit history for a request, including intake, triage, edits, assignment,
approval, work creation, and status changes. Events contain trusted tenant and actor identity,
timestamp, event type, correlation identifier, and safe business context.

## 7. Why ServiceRequest must be separate from WorkItem

`ServiceRequest` and `WorkItem` have different meanings and lifecycles:

- a service request preserves the original customer or employee intent;
- a work item describes an internal action chosen by the business;
- one request may create no work, one work item, or several work items;
- a request can be rejected, duplicated, clarified, or deferred without becoming execution work;
- request intake may contain requester identity and unstructured text that should not be copied
  into every internal task;
- approval concerns the interpretation or action proposed for a request, not merely a task status;
- audit history must show the transformation from raw intake to approved work.

Overloading `WorkItem` with requester, intake-channel, recommendation, approval, and audit concerns
would erase this boundary and make later email or webhook intake difficult. The existing
`WorkItem` model should remain the execution model, with an explicit relationship added only when
approved request work is generated.

## 8. Request workflow states

The MVP request workflow is:

| State | Meaning | Allowed next states |
|---|---|---|
| `NEW` | Raw intake was captured and awaits review | `TRIAGED`, `REJECTED` |
| `TRIAGED` | The request has an owner-reviewed interpretation and proposed next steps | `APPROVED`, `REJECTED`, `NEW` |
| `APPROVED` | A human accepted the proposed handling and work may be created | `IN_PROGRESS`, `REJECTED` |
| `REJECTED` | A human decided not to proceed; the reason remains auditable | Terminal, or `NEW` through an explicit reopen event |
| `IN_PROGRESS` | Approved execution work is underway | `DONE`, `APPROVED` |
| `DONE` | The request and its required execution work are complete | Terminal, or `IN_PROGRESS` through an explicit reopen event |

Transitions must be validated by the backend and recorded as request events. Direct database or UI
changes must not silently bypass the workflow. The first code slice uses only `NEW`; additional
transitions should arrive with their behavior and tests.

## 9. Must-have MVP features

- MissionOps AI product positioning understandable to a non-technical owner.
- Tenant-scoped manual request intake that preserves the raw request.
- Request inbox and request detail view.
- Requester type, source, workflow status, accepted priority, and timestamps.
- Automation-assisted summary, classification, priority, and suggested next steps with honest
  rule-based labeling until a real LLM exists.
- Human review and explicit approval or rejection before consequential actions.
- Idempotent creation of one or more work items linked to an approved request.
- Assignment and visible responsibility for active requests or generated work.
- Validated status transitions.
- Append-only, tenant-scoped audit history visible from the request.
- Search or basic filtering by status and priority once the inbox contains realistic volume.
- Realistic pilot/demo data and one complete Playwright customer journey.
- Structured errors, strict tenant isolation, and backend tests for every new behavior.
- A repeatable deployment and recovery procedure suitable for the pilot's data.

## 10. Explicit non-goals

The first MVP will not include:

- a full CRM, case-management platform, ERP, or project-management replacement;
- complex email ingestion or bidirectional mailbox synchronization;
- SMS, WhatsApp, voice transcription, or social-channel intake;
- attachments, document generation, e-signature, or document management;
- calendar scheduling, route optimization, payroll, or accounting integrations;
- advanced analytics, custom report builders, or configurable workflow designers;
- a native mobile application;
- autonomous high-impact actions without human approval;
- a multi-provider agent marketplace or unrestricted shell/database tools;
- a claim of real LLM intelligence before an LLM integration and evaluation exist;
- self-service Stripe billing, plan upgrades, cancellation, or a billing portal for the first paid
  pilot;
- broad multi-industry customization before one pilot workflow has been validated;
- microservices or a frontend-framework rewrite solely for architectural fashion.

## 11. Acceptance criteria

The commercial MVP is acceptable when:

1. A user can manually submit a customer, employee, or owner request with realistic unstructured
   text.
2. The original text is persisted unchanged and can be viewed later.
3. Users can list and open only requests belonging to their authenticated business.
4. Cross-tenant listing and direct-ID access are proven impossible by automated tests.
5. The product proposes a summary, category, priority, and next steps, clearly labeled according to
   whether the implementation is rule-based or uses a real LLM.
6. A human can edit, approve, reject, or defer the proposal before work is created.
7. Approval retries cannot create duplicate work.
8. Approved requests can create linked work items without changing the meaning of `WorkItem`.
9. Valid workflow transitions succeed; invalid transitions return structured errors.
10. A request timeline attributes material changes to a tenant, actor, time, and correlation ID.
11. The main local backend and Playwright verification gates pass.
12. One pilot customer can complete the demo flow without developer terminology or operator help.
13. Pilot data can be exported or deleted on request and restored from a tested backup procedure.

## 12. First code PR plan

The first code PR after this contract will deliver one narrow vertical slice: create and list
tenant-scoped manual service requests.

In scope:

- a Flyway migration introducing a `service_request` table with tenant ownership, manual intake
  fields, `NEW` status, timestamps, and a version;
- a `ServiceRequest` entity plus repository, service, request DTO, and controller following existing
  Spring patterns;
- `POST /api/service-requests` for validated manual intake;
- `GET /api/service-requests` for tenant-scoped newest-first listing;
- a small dashboard intake form and request-inbox section using customer-facing language;
- backend create/list tests;
- a tenant-isolation test covering both list and direct access boundaries as appropriate to the
  slice;
- one Playwright journey that submits a request and sees it in the inbox;
- minimal API and README documentation needed to explain the new behavior.

Out of scope for that PR: recommendation generation, approval, work-item creation, request detail,
email intake, billing changes, broad branding, and redesign. Those should follow as separately
reviewable vertical slices.

## 13. Security and privacy considerations

- Tenant identity must come only from verified authentication context, never request payloads.
- Repository reads and writes must require the trusted tenant identifier; direct-ID access to
  another tenant must behave as not found.
- Treat raw request text and requester information as potentially sensitive customer or employee
  data.
- Collect the minimum requester data needed for the pilot and document why each field exists.
- Define retention, export, correction, and deletion procedures before storing real pilot data.
- Do not send request text to an external model until the provider, data-use terms, redaction,
  retention, regional processing, and customer consent have been reviewed.
- Never place secrets, access tokens, invitation tokens, raw customer data, or hidden model
  reasoning in logs or audit events.
- Authorization must be enforced in controllers/services/tool layers rather than instructions to an
  automation or model.
- Human approvals must record the authenticated actor and be safe to retry.
- Rate limiting, request-size limits, dependency scanning, secret rotation, backup restoration, and
  incident handling must be operational before wider availability.
- Audit history should be append-only and should record business decisions without copying more
  sensitive text than necessary.

## 14. Risks before pilot launch

- **Customer-fit risk:** a generic multi-industry workflow may solve no pilot's daily problem well;
  select one request pattern and observe real usage.
- **Identity risk:** the current invitation flow does not complete Cognito tenant/group transfer;
  launch single-owner first or finish and test that external identity operation.
- **Data-model risk:** putting intake fields on `WorkItem` would couple raw requests to execution and
  make later channels and approvals harder.
- **Expectation risk:** rule-based classifications may be mistaken for a real LLM if product copy is
  vague. Label the behavior honestly.
- **Privacy risk:** free-text requests may contain health, legal, immigration, financial, employee,
  or other sensitive information beyond the intended pilot scope.
- **Audit risk:** the existing mutable agent-run record is evidence of current state, not a complete
  append-only request history.
- **Automation risk:** duplicate, stale, or unauthorized approvals could create unintended work
  unless transitions and side effects are idempotent.
- **Operational risk:** the production Cognito, cross-origin frontend/API, Stripe, invitation, backup,
  and restore journeys have not all been exercised end to end.
- **Cost risk:** the production-shaped AWS stack may cost more and require more operations than the
  first pilot justifies.
- **Commercial risk:** self-service billing can distract from validating whether customers will pay
  for request intake and follow-through; manual invoicing is the pilot default.
- **Repository risk:** the public portfolio repository and absence of an explicit license strategy
  should be reviewed before commercial customer data or proprietary integrations are introduced.

## 15. Definition of done

The first commercial MVP is done when:

- the entire demo flow in section 5 works with realistic pilot data;
- `ServiceRequest` remains the raw-intake aggregate and `WorkItem` remains the execution aggregate;
- every material request change is tenant-scoped, attributable, and visible in audit history;
- risky actions require explicit, authorized, idempotent human approval;
- automation and AI capabilities are described accurately;
- a non-technical pilot user can understand what came in, what is urgent, who owns it, what is
  recommended, what needs approval, and what already happened;
- relevant unit, integration, security, database, and Playwright tests pass behind the existing
  quality gate;
- the production pilot path has verified authentication, tenant isolation, monitoring, backup,
  restore, data export/deletion, and incident procedures;
- no secrets or unnecessary sensitive data are committed, logged, or exposed;
- pilot scope, onboarding, support, pricing, manual invoicing, and success measures are documented;
- one real pilot customer can use the product for 30 days and provide evidence about time saved,
  requests completed, missed follow-ups, approval usefulness, and willingness to continue paying.
