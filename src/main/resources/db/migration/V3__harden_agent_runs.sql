alter table agent_run add column idempotency_key varchar(80);
update agent_run set idempotency_key = cast(id as varchar(80));
alter table agent_run alter column idempotency_key set not null;
alter table agent_run add column tool_budget integer not null default 3;
alter table agent_run add column work_item_ids varchar(240) not null default '';
alter table agent_run add column approved_at timestamp with time zone;
alter table agent_run add column approved_by varchar(160);
alter table agent_run add column version bigint not null default 0;

alter table agent_run add constraint uq_agent_run_tenant_idempotency
    unique (tenant_id, idempotency_key);
alter table agent_run add constraint ck_agent_run_tool_budget
    check (tool_budget between 1 and 3);
