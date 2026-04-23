#!/usr/bin/env bash
set -euo pipefail
# mongodump --db guthi --out mongo-snapshots/bson-dump  (creates bson-dump/guthi/)
GUTHI_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SNAP="$GUTHI_ROOT/mongo-snapshots"
DUMP_OUT="$SNAP/bson-dump"
TOOLS_URL="https://www.mongodb.com/try/download/database-tools"
mkdir -p "$SNAP"
rm -rf "$DUMP_OUT" "$SNAP/guthi-bson-dump"

if command -v mongodump >/dev/null 2>&1; then
  URI="${MONGODB_URI:-mongodb://localhost:27017}"
  echo "mongodump --db guthi --out $DUMP_OUT"
  echo "(URI: $URI)"
  mongodump --uri="$URI" --db=guthi --out="$DUMP_OUT"
  echo "Done. Data under: $DUMP_OUT/guthi/"
  exit 0
fi

if [[ "${GUTHI_NO_DOCKER:-}" == "1" ]]; then
  echo "mongodump not on PATH and GUTHI_NO_DOCKER=1. Install: $TOOLS_URL" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "mongodump and docker missing. Install: $TOOLS_URL" >&2
  exit 1
fi

REMOTE="/tmp/guthi-bson-dump"
(cd "$GUTHI_ROOT" && docker compose exec -T mongo sh -c "rm -rf '$REMOTE' && mkdir -p '$REMOTE'")
(cd "$GUTHI_ROOT" && docker compose exec -T mongo mongodump --db=guthi --out="$REMOTE")
(cd "$GUTHI_ROOT" && docker compose cp "mongo:$REMOTE" "$SNAP")
(cd "$GUTHI_ROOT" && docker compose exec -T mongo sh -c "rm -rf '$REMOTE'" 2>/dev/null || true)
if [[ -d "$SNAP/guthi-bson-dump" ]]; then
  mv "$SNAP/guthi-bson-dump" "$DUMP_OUT"
fi
echo "Done. Data under: $DUMP_OUT/guthi/"
