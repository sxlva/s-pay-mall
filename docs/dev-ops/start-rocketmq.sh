#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

docker compose --env-file ./.env -f docs/dev-ops/docker-compose-environment.yml up -d

