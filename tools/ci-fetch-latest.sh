#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# 이 스크립트는 현재 저장소(~/myapp)에서 실행된다고 가정
# 사용법: tools/ci-fetch-latest.sh [브랜치]   (기본: main)
BRANCH="${1:-main}"
ART_NAME="${ART_NAME:-myapp-build-outputs}"

cd "$(dirname "$0")/.."

echo "[1/4] 최신 워크플로우 실행 대기 (branch: $BRANCH)"
RUN_ID="$(gh run list --branch "$BRANCH" --limit 1 --json databaseId -q '.[0].databaseId')"
if [ -z "${RUN_ID:-}" ]; then
  echo "❌ 해당 브랜치에 실행 기록이 없습니다."; exit 1
fi
gh run watch "$RUN_ID" --exit-status

echo "[2/4] 이전 artifacts 정리"
rm -rf artifacts && mkdir -p artifacts

echo "[3/4] 아티팩트 다운로드: $ART_NAME"
gh run download "$RUN_ID" -n "$ART_NAME" -D artifacts

echo "[3.5/4] APK 경로 확인"
APK="$(ls -1 artifacts/apk/*/*.apk | head -n1 || true)"
if [ -z "${APK:-}" ]; then
  echo "❌ APK를 찾지 못했습니다. artifacts 트리를 출력합니다:"
  ls -R artifacts
  exit 1
fi
echo "  -> $APK"

echo "[4/4] Download 폴더로 복사"
# termux-setup-storage 를 이미 한 상태여야 함
cp "$APK" ~/storage/downloads/
echo "✅ 완료: ~/storage/downloads/$(basename "$APK") 로 복사했습니다."
