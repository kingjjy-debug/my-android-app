#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# 스크립트 실제 경로 기준으로 ROOT_DIR 계산 (심볼릭 링크 대응)
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
ROOT_DIR="$(cd "$(dirname "$SCRIPT_PATH")/.." && pwd)"

BRANCH="${1:-main}"
ART_NAME="${ART_NAME:-myapp-build-outputs}"

cd "$ROOT_DIR"
echo "== [build-apk] 코드 생성 없이 CI 빌드만 실행 =="

# 빈 커밋으로 CI 트리거 (변경 없어도 동작)
git add -A
git rev-parse --abbrev-ref HEAD | grep -q "^$BRANCH$" || git checkout -B "$BRANCH"
git commit --allow-empty -m "ci: build-only $(date +'%F %T')" || true
git push -u origin "$BRANCH"

echo "== 워크플로우 대기 및 RUN_ID 조회 =="
RUN_ID="$(gh run list --branch "$BRANCH" --limit 1 --json databaseId -q '.[0].databaseId')"
if [ -z "${RUN_ID:-}" ]; then echo "❌ RUN_ID를 찾지 못했습니다."; exit 1; fi
gh run watch "$RUN_ID" --exit-status || true

echo "== 아티팩트 다운로드 =="
rm -rf "$ROOT_DIR/artifacts" && mkdir -p "$ROOT_DIR/artifacts"
gh run download "$RUN_ID" -n "$ART_NAME" -D "$ROOT_DIR/artifacts" || { echo "❌ 아티팩트 다운로드 실패"; exit 1; }

APK="$(ls -1 "$ROOT_DIR"/artifacts/apk/*/*.apk 2>/dev/null | head -n1 || true)"
if [ -z "${APK:-}" ]; then
  echo "❌ APK가 없습니다. artifacts 트리:"
  ls -R "$ROOT_DIR/artifacts" || true
  exit 1
fi

echo "== APK 복사 =="
cp -f "$APK" ~/storage/downloads/app-latest.apk
echo "✅ 완료: ~/storage/downloads/app-latest.apk"
