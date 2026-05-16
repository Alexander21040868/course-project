#!/usr/bin/env bash
# Проверка цепочки: логин → каталог задач → посылка (компиляция в Docker при docker compose).
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
USER="${CQ_USER:-student}"
PASS="${CQ_PASS:-student}"

echo "Ожидание ${BASE} ..."
for i in $(seq 1 90); do
  if curl -sf -o /dev/null "${BASE}/index.html"; then
    break
  fi
  sleep 1
  if [[ "$i" -eq 90 ]]; then
    echo "Сервер не ответил за 90 с" >&2
    exit 1
  fi
done

echo "Вход как ${USER} ..."
TOKEN="$(
  curl -sS -X POST "${BASE}/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${USER}\",\"password\":\"${PASS}\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])"
)"

echo "Первая задача из каталога ..."
TID="$(
  curl -sS "${BASE}/api/tasks?page=0&size=1" \
    -H "Authorization: Bearer ${TOKEN}" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['content'][0]['id'])"
)"

echo "Посылка по задаче #${TID} ..."
BODY="$(python3 - "$TID" <<'PY'
import json, sys
tid = int(sys.argv[1])
code = r"""#include <stdio.h>

int main() {
    printf("Hello");
    return 0;
}
"""
print(json.dumps({"taskId": tid, "code": code}))
PY
)"

RESP="$(curl -sS -X POST "${BASE}/api/submissions" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "${BODY}")"

echo "$RESP" | python3 -m json.tool

STATUS="$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))")"
SANDBOX="$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('codeSandbox') or '')")"
if [[ "$STATUS" != "CORRECT" ]]; then
  echo "Ожидался status=CORRECT, получено: ${STATUS}" >&2
  exit 1
fi
if [[ -n "${EXPECT_DOCKER:-}" && "$EXPECT_DOCKER" == "1" && "$SANDBOX" != "DOCKER" ]]; then
  echo "Ожидался codeSandbox=DOCKER (проверка через контейнер), получено: ${SANDBOX:-пусто}" >&2
  exit 1
fi

echo "OK: codeSandbox=${SANDBOX:-null}, status=${STATUS}"
