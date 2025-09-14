#!/data/data/com.termux/files/usr/bin/bash
# Usage: tools/build-and-fetch.sh [branch]
# Default branch: main
set -euo pipefail

BRANCH="${1:-main}"
ART_NAME="${ART_NAME:-myapp-build-outputs}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "$ROOT_DIR"

# 필수 도구 점검
command -v gh >/dev/null || { echo "❌ gh CLI가 필요합니다 (pkg install gh 등)"; exit 1; }
command -v node >/dev/null || { echo "❌ Node.js가 필요합니다 (pkg install nodejs 등)"; exit 1; }

# Gemini API Key (사용자 요청으로 내장)
export GEMINI_API_KEY="AIzaSyBH4HP4gytUm54PDBBXWQXZ1GDqFW8w5aE"

# 요구사항 파일 확인
[ -f app_description.txt ] || { echo "❌ app_description.txt 가 없습니다."; exit 1; }

# Download 권한(최초 1회)
[ -d "$HOME/storage/downloads" ] || termux-setup-storage || true

echo "[1/6] 코드 생성 (Gemini)"
node tools/gen-from-description.js app_description.txt

# 프리플라이트 안정화
chmod +x "$ROOT_DIR/tools/preflight-stabilize.sh" 2>/dev/null || true
"$ROOT_DIR/tools/preflight-stabilize.sh" || true
echo "✅ preflight-stabilize: 기본 안정화 완료"

echo "[2/6] Git 커밋 & 푸시"
git add -A
git commit -m "chore: auto-generate code ($(date +'%Y-%m-%d %H:%M:%S'))" || echo "ℹ️ 변경 없음(커밋 생략)"

# 브랜치 정렬
if ! git rev-parse --abbrev-ref HEAD | grep -q "^${BRANCH}\$"; then
  git checkout -B "$BRANCH"
fi
git push -u origin "$BRANCH"

# 방금 푼 커밋 SHA
HEAD_SHA="$(git rev-parse HEAD)"

echo "[3/6] 최신 워크플로우 실행 대기 (HEAD=$HEAD_SHA, branch=$BRANCH)"
# 방금 커밋(HEAD_SHA)로 생성된 실행을 '등장할 때까지' 폴링
RUN_ID=""
for i in {1..60}; do
  RUN_ID="$(gh run list \
    --branch "$BRANCH" \
    --workflow "Android CI" \
    --json databaseId,headSha,status,createdAt \
    --limit 30 \
    -q 'map(select(.headSha=="'"$HEAD_SHA"'")) | sort_by(.createdAt) | reverse | .[0].databaseId' || true)"
  if [ -n "${RUN_ID:-}" ] && [ "$RUN_ID" != "null" ]; then
    break
  fi
  echo "  - 대기중… (방금 커밋 실행 아직 생성 전)"; sleep 3
done

if [ -z "${RUN_ID:-}" ] || [ "$RUN_ID" = "null" ]; then
  echo "❌ 방금 커밋($HEAD_SHA)으로 생성된 실행을 찾지 못했습니다."
  echo "   힌트: GitHub Actions에서 워크플로 이름이 'Android CI'인지 확인하세요."
  exit 1
fi
echo "  -> RUN_ID=$RUN_ID 발견. 진행 중 상태를 모니터링합니다."

# 해당 실행이 끝날 때까지 모니터링
gh run watch "$RUN_ID" --exit-status

echo "[4/6] 이전 artifacts 정리"
rm -rf artifacts && mkdir -p artifacts

echo "[5/6] 아티팩트 다운로드 ($ART_NAME)"
gh run download "$RUN_ID" -n "$ART_NAME" -D artifacts

echo "[5.5/6] APK 경로 확인"
APK="$(ls -1 artifacts/apk/*/*.apk | head -n1 || true)"
if [ -z "${APK:-}" ]; then
  echo "❌ APK를 찾지 못했습니다. 아래 트리를 확인하세요:"
  ls -R artifacts || true
  exit 1
fi
echo "  -> $APK"

echo "[6/6] Download 폴더로 복사 (덮어쓰기)"
mkdir -p "$HOME/storage/downloads"
cp -f "$APK" "$HOME/storage/downloads/app-latest.apk"
echo "✅ 완료: $HOME/storage/downloads/app-latest.apk"
