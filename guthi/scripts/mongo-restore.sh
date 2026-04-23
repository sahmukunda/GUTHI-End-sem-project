#!/usr/bin/env bash
set -euo pipefail
GUTHI_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DUMP_DIR="$GUTHI_ROOT/mongo-snapshots/bson-dump"
TOOLS_URL="https://www.mongodb.com/try/download/database-tools"

if [[ ! -d "$DUMP_DIR/guthi" ]]; then
  echo "Missing: $DUMP_DIR/guthi" >&2
  exit 1
fi

if command -v mongorestore >/dev/null 2>&1; then
  URI="${MONGODB_URI:-mongodb://localhost:27017}"
  echo "mongorestore --drop --uri $URI $DUMP_DIR"
  mongorestore --uri="$URI" --drop "$DUMP_DIR"
  echo "Done."
  exit 0
fi

if [[ "${GUTHI_NO_DOCKER:-}" == "1" ]]; then
  echo "mongorestore not on PATH. Install: $TOOLS_URL" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "mongorestore and docker missing. Install: $TOOLS_URL" >&2
  exit 1
fi

REMOTE="/tmp/guthi-bson-restore"
(cd "$GUTHI_ROOT" && docker compose exec -T mongo sh -c "rm -rf '$REMOTE'" 2>/dev/null || true)
(cd "$GUTHI_ROOT" && docker compose cp "$DUMP_DIR" "mongo:$REMOTE")
(cd "$GUTHI_ROOT" && docker compose exec -T mongo mongorestore --drop "$REMOTE")
(cd "$GUTHI_ROOT" && docker compose exec -T mongo sh -c "rm -rf '$REMOTE'" 2>/dev/null || true)
echo "Done."
