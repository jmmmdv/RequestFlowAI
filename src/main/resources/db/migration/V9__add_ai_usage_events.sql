create table ai_usage_event (
    id uuid primary key,
    tenant_id uuid not null references tenant(id),
    organization_slug varchar(80),
    request_id uuid references request_submission(id) on delete set null,
    agent_run_id uuid references agent_run(id) on delete set null,
    operation varchar(40) not null,
    analysis_source varchar(30) not null,
    model_name varchar(80),
    estimated_input_tokens integer,
    estimated_output_tokens integer,
    estimated_total_tokens integer,
    estimated_cost_usd numeric(12, 6) not null default 0,
    budget_status varchar(30) not null,
    paid_ai_used boolean not null default false,
    fallback_used boolean not null default false,
    created_at timestamp with time zone not null,
    constraint ck_ai_usage_event_operation check (
        operation in ('REQUEST_ANALYSIS', 'AGENT_ANALYSIS')),
    constraint ck_ai_usage_event_analysis_source check (
        analysis_source in ('RULE_BASED', 'LLM', 'FALLBACK')),
    constraint ck_ai_usage_event_budget_status check (
        budget_status in ('OK', 'WARNING', 'HARD_STOP', 'DISABLED'))
);

create index ix_ai_usage_event_tenant_created on ai_usage_event (tenant_id, created_at desc);
create index ix_ai_usage_event_created on ai_usage_event (created_at desc);
