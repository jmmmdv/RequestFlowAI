create table tenant (
    id uuid primary key,
    name varchar(120) not null,
    slug varchar(80) not null unique,
    created_at timestamp with time zone not null
);

insert into tenant (id, name, slug, created_at)
values ('00000000-0000-0000-0000-000000000001', 'Local Development', 'local', current_timestamp);

alter table work_item add column tenant_id uuid;
update work_item set tenant_id = '00000000-0000-0000-0000-000000000001';
alter table work_item alter column tenant_id set not null;
alter table work_item add constraint fk_work_item_tenant foreign key (tenant_id) references tenant(id);
create index ix_work_item_tenant_updated on work_item (tenant_id, updated_at desc);

create table agent_run (
    id uuid primary key,
    tenant_id uuid not null references tenant(id),
    user_id varchar(160) not null,
    correlation_id varchar(64) not null,
    goal varchar(500) not null,
    classification varchar(80) not null,
    outcome varchar(30) not null,
    created_work_items integer not null,
    created_at timestamp with time zone not null
);

create index ix_agent_run_tenant_created on agent_run (tenant_id, created_at desc);
