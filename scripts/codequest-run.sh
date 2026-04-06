#!/usr/bin/env bash
# Запуск Spring Boot с Docker CLI в PATH (на macOS ссылка /usr/local/bin/docker часто ведёт на отключённый том).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -x "/Applications/Docker.app/Contents/Resources/bin/docker" ]]; then
  export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"
fi
cd "$ROOT"
exec mvn spring-boot:run "$@"
