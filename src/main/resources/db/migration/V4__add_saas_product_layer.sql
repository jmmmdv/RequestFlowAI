alter table tenant add column plan varchar(20) not null default 'FREE';
alter table tenant add column status varchar(20) not null default 'ACTIVE';
alter table tenant add constraint ck_tenant_plan check (plan in ('FREE', 'PRO', 'BUSINESS'));
alter table tenant add constraint ck_tenant_status check (status in ('ACTIVE', 'SUSPENDED'));

create table tenant_membership (
    id uuid primary key,
    tenant_id uuid not null references tenant(id),
    user_id varchar(160) not null,
    email varchar(254),
    role varchar(20) not null,
    joined_at timestamp with time zone not null,
    constraint uq_membership_tenant_user unique (tenant_id, user_id),
    constraint ck_membership_role check (role in ('ADMIN', 'MEMBER', 'VIEWER'))
);
create index ix_membership_tenant on tenant_membership (tenant_id, joined_at);

create table tenant_invitation (
    id uuid primary key,
    tenant_id uuid not null references tenant(id),
    email varchar(254) not null,
    role varchar(20) not null,
    token_hash varchar(64) not null unique,
    invited_by varchar(160) not null,
    expires_at timestamp with time zone not null,
    accepted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    constraint ck_invitation_role check (role in ('ADMIN', 'MEMBER', 'VIEWER'))
);
create index ix_invitation_tenant on tenant_invitation (tenant_id, created_at desc);

create table billing_subscription (
    tenant_id uuid primary key references tenant(id),
    stripe_customer_id varchar(120),
    stripe_subscription_id varchar(120) unique,
    plan varchar(20) not null,
    status varchar(30) not null,
    current_period_end timestamp with time zone,
    updated_at timestamp with time zone not null,
    constraint ck_subscription_plan check (plan in ('FREE', 'PRO', 'BUSINESS'))
);

insert into billing_subscription (tenant_id, plan, status, updated_at)
select id, plan, 'ACTIVE', current_timestamp from tenant;
