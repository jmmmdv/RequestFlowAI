#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
STACK="${STACK_NAME:-automation-mission-control}"
REGION="${AWS_REGION:-us-east-1}"
SOURCE_DB="${SOURCE_DB:-automation-mission-control-database-jyw6fy4boq4f}"

echo "RequestFlow AI — RDS backup readiness check"
echo "Full restore drill: $ROOT/docs/operations/RESTORE-DRILL.md"
echo

if ! command -v aws >/dev/null 2>&1; then
  echo "AWS CLI required." >&2
  exit 1
fi

INFO="$(aws rds describe-db-instances \
  --region "$REGION" \
  --db-instance-identifier "$SOURCE_DB" \
  --query 'DBInstances[0].{Status:DBInstanceStatus,BackupRetention:BackupRetentionPeriod,LatestRestorableTime:LatestRestorableTime,MultiAZ:MultiAZ,Encrypted:StorageEncrypted,DeletionProtection:DeletionProtection,Engine:Engine,EngineVersion:EngineVersion}' \
  --output json)"

echo "$INFO" | jq .
STATUS="$(echo "$INFO" | jq -r .Status)"
RETENTION="$(echo "$INFO" | jq -r .BackupRetention)"
RESTORABLE="$(echo "$INFO" | jq -r .LatestRestorableTime)"

if [[ "$STATUS" != "available" ]]; then
  echo "Database is not available ($STATUS)." >&2
  exit 1
fi
if [[ "$RETENTION" -lt 1 ]]; then
  echo "Backup retention must be >= 1 day before restore drill." >&2
  exit 1
fi
if [[ "$RESTORABLE" == "null" ]]; then
  echo "LatestRestorableTime is missing — wait for first backup window." >&2
  exit 1
fi

echo
echo "Backup prerequisites look good."
echo "Next: run ./scripts/drills/restore-drill.sh when ready for a timed restore exercise."
echo "Record results with: ./scripts/drills/record-drill.sh aws-restore pass ..."
