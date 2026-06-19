alter table request_submission
    add column company_name varchar(160) not null default 'Not provided';

alter table request_submission alter column company_name drop default;

alter table request_submission add column requested_category varchar(40);
alter table request_submission add column requested_urgency varchar(20);

alter table request_submission add constraint ck_request_submission_requested_category check (
    requested_category is null or requested_category in
        ('SUPPORT', 'BILLING', 'SALES', 'CHANGE_REQUEST', 'GENERAL'));

alter table request_submission add constraint ck_request_submission_requested_urgency check (
    requested_urgency is null or requested_urgency in ('LOW', 'NORMAL', 'HIGH', 'URGENT'));

alter table request_submission drop constraint ck_request_submission_status;
update request_submission set status = 'NEW' where status = 'RECEIVED';
alter table request_submission add constraint ck_request_submission_status check (
    status in ('NEW', 'ARCHIVED'));
