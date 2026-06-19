create table request_submission (
    id uuid primary key,
    tenant_id uuid not null references tenant(id),
    idempotency_key varchar(80) not null,
    requester_name varchar(120) not null,
    requester_email varchar(254) not null,
    title varchar(120) not null,
    details varchar(2000) not null,
    category varchar(40) not null,
    suggested_priority varchar(20) not null,
    internal_summary varchar(500) not null,
    recommended_next_action varchar(500) not null,
    status varchar(20) not null,
    work_item_id bigint references work_item(id) on delete set null,
    created_at timestamp with time zone not null,
    constraint uq_request_submission_idempotency unique (tenant_id, idempotency_key),
    constraint ck_request_submission_category check (
        category in ('SUPPORT', 'BILLING', 'SALES', 'CHANGE_REQUEST', 'GENERAL')),
    constraint ck_request_submission_priority check (
        suggested_priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    constraint ck_request_submission_status check (status in ('RECEIVED', 'ARCHIVED'))
);

create index ix_request_submission_tenant_created
    on request_submission (tenant_id, created_at desc);
