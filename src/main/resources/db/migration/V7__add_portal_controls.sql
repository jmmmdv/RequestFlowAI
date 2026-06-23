alter table tenant add column portal_token_hash varchar(64);
alter table tenant add column request_retention_days integer not null default 365;
alter table tenant add column onboarding_completed boolean not null default true;

alter table tenant add constraint ck_tenant_retention_days check (request_retention_days between 7 and 2555);
