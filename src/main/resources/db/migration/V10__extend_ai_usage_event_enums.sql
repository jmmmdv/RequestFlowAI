alter table ai_usage_event drop constraint ck_ai_usage_event_operation;
alter table ai_usage_event add constraint ck_ai_usage_event_operation check (
    operation in ('REQUEST_ANALYSIS', 'AGENT_ANALYSIS', 'PUBLIC_INTAKE_CLASSIFICATION'));

alter table ai_usage_event drop constraint ck_ai_usage_event_budget_status;
alter table ai_usage_event add constraint ck_ai_usage_event_budget_status check (
    budget_status in ('OK', 'WARNING', 'HARD_STOP', 'DISABLED', 'NOT_PAID_AI'));
