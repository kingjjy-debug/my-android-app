#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "== [build-apk] 코드 생성 없이 CI 빌드만 실행 =="

# 1) 빈 커밋으로 CI 트리거 (변경 없을 때도 강제 트리거)
git commit --allow-empty -m "ci: build-only $(date +'%Y-%m-%d %H:%M:%S')"
git push -u origin "$(git rev-parse --abbrev-ref HEAD)"

# 2) 방금 푸시한 커밋의 SHA (여기서 읽어야 함!)
HEAD_SHA="$(git rev-parse HEAD)"
echo "HEAD_SHA=$HEAD_SHA"

# 3) 이 SHA로 뜬 RUN을 기다림
echo "== 워크플로우 대기 및 RUN_ID 조회 =="
RUN_ID=""
for i in {1..120}; do
  RUN_ID="$(gh run list --limit 30 --json databaseId,headSha,status,displayTitle \
    -q "map(select(.headSha==\"$HEAD_SHA\"))[0].databaseId" || true)"
  if [ -n "${RUN_ID:-}" ]; then
    break
  fi
  sleep 2
done
[ -n "${RUN_ID:-}" ] || { echo "❌ 새 커밋(${HEAD_SHA})에 대한 run을 찾지 못했습니다."; exit 1; }

# 4) 해당 RUN 완료까지 모니터
if ! gh run watch "$RUN_ID" --exit-status; then
  echo "❌ CI 실패. 핵심 로그:"
  JOB_ID="$(gh run view "$RUN_ID" --json jobs -q '.jobs[-1].databaseId' || true)"
  [ -n "${JOB_ID:-}" ] && gh run view "$RUN_ID" --job "$JOB_ID" --log | \
    grep -E 'AAPT|Android resource linking failed|error: resource|Execution failed for task|FAILURE: Build failed' || true
  exit 1
fi

# 5) 아티팩트 다운로드
echo "== 아티팩트 다운로드 =="
rm -rf artifacts && mkdir -p artifacts
gh run download "$RUN_ID" -n myapp-build-outputs -D artifacts

# 6) APK 복사
APK="$(ls -1 artifacts/apk/*/*.apk | head -n1 || true)"
[ -n "${APK:-}" ] || { echo "❌ APK 파일을 찾지 못했습니다."; ls -R artifacts; exit 1; }
mkdir -p ~/storage/downloads
cp -f "$APK" ~/storage/downloads/app-latest.apk
echo "✅ APK 복사 완료: ~/storage/downloads/app-latest.apk"
