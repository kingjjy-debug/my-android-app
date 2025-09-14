#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "== [gen] app_description.txt 기반 코드 생성 (gemini-2.5-pro) =="
node "$ROOT_DIR/tools/gen-from-description.js" app_description.txt
"$ROOT_DIR/tools/preflight-stabilize.sh"
echo "✅ gen-code 완료 (ROOT_DIR=$ROOT_DIR)"
