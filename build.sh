#!/bin/bash
# 항상 main 브랜치로 실행
bash ~/myapp/tools/build-and-fetch.sh main

# [6/6] APK 다운로드 및 복사
echo "[6/6] 최신 APK 다운로드 및 복사"

# 아티팩트 폴더 초기화
rm -rf ~/myapp/artifacts
mkdir -p ~/myapp/artifacts

# 아티팩트 다운로드
gh run download "$RUN_ID" -n myapp-build-outputs -D ~/myapp/artifacts || {
  echo "❌ APK 다운로드 실패"
  exit 1
}

# APK 경로 찾기
APK_PATH="$(find ~/myapp/artifacts -name '*.apk' | head -n1)"

if [ -n "$APK_PATH" ]; then
  # Downloads 폴더에 덮어쓰기 복사
  cp -f "$APK_PATH" ~/storage/downloads/app-latest.apk
  echo "✅ APK 복사 완료: ~/storage/downloads/app-latest.apk"
else
  echo "❌ APK 파일을 찾지 못했습니다."
  exit 1
fi
