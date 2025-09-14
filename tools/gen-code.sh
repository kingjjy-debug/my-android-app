#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# 스크립트 실제 경로 기준으로 ROOT_DIR 계산 (심볼릭 링크 대응)
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
ROOT_DIR="$(cd "$(dirname "$SCRIPT_PATH")/.." && pwd)"

cd "$ROOT_DIR"

echo "== [gen] app_description.txt 기반 코드 생성 (gemini-2.5-pro) =="
node "$ROOT_DIR/tools/gen-from-description.js" app_description.txt

# 안정화 단계
"$ROOT_DIR/tools/preflight-stabilize.sh"

echo "✅ gen-code 완료 (ROOT_DIR=$ROOT_DIR)"
