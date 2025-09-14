#!/data/data/com.termux/files/usr/bin/bash
# Usage: tools/build-and-fetch.sh [branch]
# Default branch: main
set -euo pipefail

BRANCH="${1:-main}"
ART_NAME="${ART_NAME:-myapp-build-outputs}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

cd "$ROOT_DIR"

### 0) 사전 점검 ##############################################################

# 필수 바이너리 확인
command -v gh >/dev/null || { echo "❌ gh CLI가 필요합니다 (pkg install gh 등)"; exit 1; }
command -v node >/dev/null || { echo "❌ Node.js가 필요합니다 (pkg install nodejs 등)"; exit 1; }

# Gemini API Key (사용자 요청으로 고정값 내장)
export GEMINI_API_KEY="AIzaSyBH4HP4gytUm54PDBBXWQXZ1GDqFW8w5aE"

# 요구사항 파일 확인
if [ ! -f "app_description.txt" ]; then
  echo "❌ app_description.txt 가 없습니다. 먼저 기능 요구사항을 작성하세요."
  exit 1
fi

# Download 폴더 접근 권한(최초 1회 필요)
if [ ! -d "$HOME/storage/downloads" ]; then
  termux-setup-storage || true
fi

### 1) 코드 생성 ##############################################################
echo "[1/6] 코드 생성 (Gemini)"
node tools/gen-from-description.js app_description.txt

# 프리플라이트 안정화
chmod +x "$ROOT_DIR/tools/preflight-stabilize.sh" 2>/dev/null || true
"$ROOT_DIR/tools/preflight-stabilize.sh"

### 2) 커밋 & 푸시 ############################################################
echo "[2/6] Git 커밋 & 푸시"
git add -A
git commit -m "chore: auto-generate code ($(date +'%Y-%m-%d %H:%M:%S'))" || echo "ℹ️ 변경 없음(커밋 생략)"
# 브랜치 정렬
if ! git rev-parse --abbrev-ref HEAD | grep -q "^${BRANCH}\$"; then
  git checkout -B "$BRANCH"
fi
git push -u origin "$BRANCH"

### 3) CI 대기 ###############################################################
echo "[3/6] 최신 워크플로우 실행 대기 (branch: $BRANCH)"
RUN_ID="$(gh run list --branch "$BRANCH" --limit 1 --json databaseId -q '.[0].databaseId')"
if [ -z "${RUN_ID:-}" ]; then
  echo "❌ 이 브랜치에 실행 기록이 없습니다."
  exit 1
fi
gh run watch "$RUN_ID" --exit-status

### 4) 이전 artifacts 정리 ####################################################
echo "[4/6] 이전 artifacts 정리"
rm -rf artifacts && mkdir -p artifacts

### 5) 아티팩트 다운로드 ######################################################
echo "[5/6] 아티팩트 다운로드: $ART_NAME"
gh run download "$RUN_ID" -n "$ART_NAME" -D artifacts

echo "[5.5/6] APK 경로 확인"
APK="$(ls -1 artifacts/apk/*/*.apk | head -n1 || true)"
if [ -z "${APK:-}" ]; then
  echo "❌ APK를 찾지 못했습니다. 아래 트리를 확인하세요:"
  ls -R artifacts || true
  exit 1
fi
echo "  -> $APK"

### 6) Download 폴더로 복사 (강제 덮어쓰기) ###################################
echo "[6/6] Download 폴더로 복사"
mkdir -p "$HOME/storage/downloads"
cp -f "$APK" "$HOME/storage/downloads/app-latest.apk"
echo "✅ 완료: $HOME/storage/downloads/app-latest.apk 로 복사했습니다."
