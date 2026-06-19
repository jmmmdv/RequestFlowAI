#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SOURCE_DB="${SOURCE_DB:-automation-mission-control}"
RESTORE_DB="${RESTORE_DB:-${SOURCE_DB}-restore-$(date +%Y%m%d%H%M)}"
AWS_REGION="${AWS_REGION:-us-east-1}"

echo "RequestFlow AI — RDS restore drill helper"
echo "Full procedure: $ROOT/docs/operations/RESTORE-DRILL.md"
echo
echo "Source DB:  $SOURCE_DB"
echo "Restore DB: $RESTORE_DB"
echo "Region:     $AWS_REGION"
echo

if ! command -v aws >/dev/null 2>&1; then
  echo "AWS CLI required." >&2
  exit 1
fi

read -r -p "Start point-in-time restore? [y/N] " confirm
[[ "$confirm" == [yY] ]] || exit 0

START_TIME="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "Drill start (UTC): $START_TIME"

aws rds restore-db-instance-to-point-in-time \
  --region "$AWS_REGION" \
  --source-db-instance-identifier "$SOURCE_DB" \
  --target-db-instance-identifier "$RESTORE_DB" \
  --use-latest-restorable-time \
  --no-publicly-accessible

echo "Waiting for restored instance..."
aws rds wait db-instance-available \
  --region "$AWS_REGION" \
  --db-instance-identifier "$RESTORE_DB"

END_TIME="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "Instance available (UTC): $END_TIME"
echo
echo "Next steps (manual):"
echo "  1. Connect from VPC; verify flyway_schema_history"
echo "  2. Compare tenant / work_item / agent_run row counts"
echo "  3. Run smoke tests against temporary App Runner revision"
echo "  4. Record RPO/RTO in docs/operations/RESTORE-DRILL.md evidence table"
echo "  5. Delete restored instance after evidence captured:"
echo "     aws rds delete-db-instance --db-instance-identifier $RESTORE_DB --skip-final-snapshot"
