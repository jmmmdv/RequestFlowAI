# PostgreSQL backup and restore drill

Run this quarterly and before materially changing the database topology. Use a non-production AWS
account and sanitized data. The target is RPO ≤24 hours and RTO ≤60 minutes.

## Prepare

1. Record drill owner, start time, source DB identifier, latest restorable time, and expected row
   counts for `tenant`, `work_item`, and `agent_run`.
2. Confirm the source instance reports `BackupRetentionPeriod >= 1` and `LatestRestorableTime`.
3. Choose an isolated restore identifier and reuse the production DB subnet/security groups. Never
   make the restored database public.

```bash
export SOURCE_DB=automation-mission-control
export RESTORE_DB="${SOURCE_DB}-restore-$(date +%Y%m%d%H%M)"
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier "$SOURCE_DB" \
  --target-db-instance-identifier "$RESTORE_DB" \
  --use-latest-restorable-time \
  --no-publicly-accessible
aws rds wait db-instance-available --db-instance-identifier "$RESTORE_DB"
```

## Validate

1. Connect from an authorized host in the VPC; retrieve credentials through Secrets Manager.
2. Run `select installed_rank, version, success from flyway_schema_history order by installed_rank`.
3. Compare the three expected row counts and inspect the newest timestamps.
4. Start a temporary application revision against the restored endpoint and run health, CRUD,
   tenant-isolation, agent idempotency, and approval smoke tests.
5. Record achieved RPO and RTO, missing data, errors, and every manual step.

## Clean up and improve

Delete the temporary App Runner revision and restored instance only after evidence is captured.
Confirm deletion explicitly—the production template enables deletion protection and snapshot
retention. File corrective work for any missed target or undocumented manual dependency.

| Drill date | RPO achieved | RTO achieved | Result | Evidence link |
|---|---:|---:|---|---|
| _YYYY-MM-DD_ | _minutes_ | _minutes_ | _pass/fail_ | _ticket/report_ |
