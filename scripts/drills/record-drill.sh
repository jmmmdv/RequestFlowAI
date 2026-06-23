#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG="$ROOT/docs/saas/DRILL-LOG.md"
STATIC_LOG="$ROOT/src/main/resources/static/docs/pilot/DRILL-LOG.md"

usage() {
  cat <<'EOF'
Usage: record-drill.sh <drill-id> <pass|fail> [options]

Drill IDs:
  cognito-signup
  cognito-invitation-transfer
  stripe-checkout
  aws-restore

Options:
  --env TEXT        Environment label (e.g. "sandbox us-east-1")
  --operator NAME   Person who ran the drill
  --notes TEXT      Short evidence summary

Example:
  ./scripts/drills/record-drill.sh cognito-signup pass \
    --env "sandbox us-east-1" \
    --operator "jeyhun" \
    --notes "JWT tenant_id matched /api/saas/organization"
EOF
}

if [[ $# -lt 2 ]]; then
  usage
  exit 1
fi

DRILL_ID="$1"
RESULT="$2"
shift 2
ENV_LABEL="—"
OPERATOR="${USER:-—}"
NOTES="—"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_LABEL="$2"; shift 2 ;;
    --operator) OPERATOR="$2"; shift 2 ;;
    --notes) NOTES="$2"; shift 2 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

case "$RESULT" in
  pass|fail) ;;
  *) echo "Result must be pass or fail" >&2; exit 1 ;;
esac

case "$DRILL_ID" in
  cognito-signup|cognito-invitation-transfer|stripe-checkout|aws-restore) ;;
  *) echo "Unknown drill id: $DRILL_ID" >&2; exit 1 ;;
esac

UTC_NOW="$(date -u +"%Y-%m-%d %H:%M UTC")"

update_table() {
  local file="$1"
  python3 - "$file" "$DRILL_ID" "$RESULT" "$UTC_NOW" "$ENV_LABEL" "$OPERATOR" "$NOTES" <<'PY'
import pathlib, re, sys
path, drill_id, result, when, env, operator, notes = sys.argv[1:8]
text = pathlib.Path(path).read_text()
status = "**pass**" if result == "pass" else "**fail**"
pattern = rf"(\| `{re.escape(drill_id)}` \|[^|]+\|)[^|]+(\|)[^|]+(\|)[^|]+(\|)[^|]+(\|)[^|]+(\|)"
replacement = rf"\1 {status} \2 {when} \3 {env} \4 {operator} \5 {notes} \6"
new_text, count = re.subn(pattern, replacement, text, count=1)
if count != 1:
    raise SystemExit(f"Could not update drill row '{drill_id}' in {path}")
pathlib.Path(path).write_text(new_text)
PY
}

append_completed() {
  local file="$1"
  python3 - "$file" "$DRILL_ID" "$RESULT" "$UTC_NOW" "$ENV_LABEL" "$OPERATOR" "$NOTES" <<'PY'
import pathlib, sys
path, drill_id, result, when, env, operator, notes = sys.argv[1:8]
text = pathlib.Path(path).read_text()
entry = (
    f"\n### {when} — `{drill_id}` ({result.upper()})\n"
    f"- Environment: {env}\n"
    f"- Operator: {operator}\n"
    f"- Notes: {notes}\n"
)
marker = "## Completed runs\n\n_No live drills recorded yet._"
if marker in text:
    text = text.replace(marker, "## Completed runs" + entry)
else:
    text = text.rstrip() + "\n" + entry
pathlib.Path(path).write_text(text + "\n")
PY
}

for target in "$LOG" "$STATIC_LOG"; do
  if [[ -f "$target" ]]; then
    update_table "$target"
    append_completed "$target"
    echo "Updated $target"
  fi
done

echo "Recorded $DRILL_ID as $RESULT at $UTC_NOW"
