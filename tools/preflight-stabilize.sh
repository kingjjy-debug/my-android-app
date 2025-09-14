#!/data/data/com.termux/files/usr/bin/bash
# Fix misplaced sources & sanitize manifest before pushing/CI
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR/app"
MAIN_DIR="$APP_DIR/src/main"

mkdir -p "$MAIN_DIR/java" "$MAIN_DIR/res" "$APP_DIR/src" "$ROOT_DIR/src" || true

echo "🔧 preflight-stabilize: 시작"

# 1) root에 잘못 생성된 src/main -> app/src/main 으로 이동
if [ -d "$ROOT_DIR/src/main" ]; then
  echo "  • root/src/main → app/src/main 으로 이동"
  rsync -a --remove-source-files "$ROOT_DIR/src/main/" "$MAIN_DIR/" || true
  # 빈 디렉터리 정리
  find "$ROOT_DIR/src" -type d -empty -delete || true
fi

# 2) root에 잘못 생성된 AndroidManifest.xml → app/src/main/AndroidManifest.xml
if [ -f "$ROOT_DIR/AndroidManifest.xml" ]; then
  echo "  • root/AndroidManifest.xml → app/src/main/AndroidManifest.xml 이동(덮어쓰기)"
  mv -f "$ROOT_DIR/AndroidManifest.xml" "$MAIN_DIR/AndroidManifest.xml"
fi

# 3) 기존 app/src/main/AndroidManifest.xml이 없다면 최소 골격 생성
if [ ! -f "$MAIN_DIR/AndroidManifest.xml" ]; then
  echo "  • AndroidManifest.xml 기본 템플릿 생성"
  cat > "$MAIN_DIR/AndroidManifest.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application
      android:allowBackup="true"
      android:supportsRtl="true"
      android:label="MyApp">
    <activity
        android:name=".MainActivity"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
XML
fi

# 4) Manifest 정리: package 속성 제거(Gradle namespace 사용), exported 누락 방지
# - package 속성은 이제 무시/경고 대상이므로 있으면 지운다.
if grep -q 'package=' "$MAIN_DIR/AndroidManifest.xml"; then
  echo "  • AndroidManifest.xml에서 package 속성 제거"
  # 단순 제거: package="..."; 를 삭제
  sed -i 's/ *package="[^"]*"//g' "$MAIN_DIR/AndroidManifest.xml"
fi

# 5) 앱 아이콘 참조 때문에 빌드가 깨지는 경우를 막기 위해,
#    manifest에 @mipmap/ic_launcher, @mipmap/ic_launcher_round 같은 참조가 있다면 우선 제거(선택)
#    * 나중에 실제 아이콘을 넣고 싶으면 values/strings와 mipmap 리소스를 추가하면 됨
if grep -q '@mipmap/ic_launcher' "$MAIN_DIR/AndroidManifest.xml" || grep -q '@mipmap/ic_launcher_round' "$MAIN_DIR/AndroidManifest.xml"; then
  echo "  • 아이콘 참조 제거(아이콘 미제공 시 빌드 실패 예방)"
  sed -i 's/android:icon="@mipmap\/ic_launcher"//g' "$MAIN_DIR/AndroidManifest.xml"
  sed -i 's/android:roundIcon="@mipmap\/ic_launcher_round"//g' "$MAIN_DIR/AndroidManifest.xml"
fi

# 6) strings.xml 최소 보장 (app_name 없으면 warning/실패 가능)
VALUES_DIR="$MAIN_DIR/res/values"
mkdir -p "$VALUES_DIR"
if [ ! -f "$VALUES_DIR/strings.xml" ]; then
  echo "  • values/strings.xml 기본 생성"
  cat > "$VALUES_DIR/strings.xml" <<'XML'
<resources>
    <string name="app_name">MyApp</string>
</resources>
XML
fi

# 7) Gradle 모듈 구조 점검(간단 체크)
if [ ! -f "$APP_DIR/build.gradle.kts" ] && [ ! -f "$APP_DIR/build.gradle" ]; then
  echo "⚠️  경고: app 모듈의 Gradle 스크립트가 없습니다. (app/build.gradle.kts 또는 app/build.gradle 확인)"
  echo "    이전에 우리가 쓰던 기본 템플릿이 삭제되지 않았는지 확인해주세요."
fi

echo "✅ preflight-stabilize: 완료"
