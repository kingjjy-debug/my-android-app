#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-$HOME/myapp}"
RES_DIR="$APP_DIR/app/src/main/res"
MANIFEST="$APP_DIR/app/src/main/AndroidManifest.xml"
STRINGS="$RES_DIR/values/strings.xml"
GRADLE_PROPS="$APP_DIR/gradle.properties"
APP_GRADLE="$APP_DIR/app/build.gradle.kts"

echo "[preflight] 시작"

# 1) gradle.properties 안전 옵션 보강
mkdir -p "$APP_DIR"
touch "$GRADLE_PROPS"
grep -q '^android.useAndroidX=true' "$GRADLE_PROPS" || echo 'android.useAndroidX=true' >> "$GRADLE_PROPS"
grep -q '^kotlin.code.style=official' "$GRADLE_PROPS" || echo 'kotlin.code.style=official' >> "$GRADLE_PROPS"

# 2) strings.xml 보장 (app_name)
mkdir -p "$RES_DIR/values"
if ! grep -q '<string name="app_name">' "$STRINGS" 2>/dev/null; then
  cat > "$STRINGS" <<'EOF'
<resources>
    <string name="app_name">MyApp</string>
</resources>
EOF
fi

# 3) 기본 런처 아이콘(적응형) 세트 주입 (있으면 건드리지 않음)
mkdir -p "$RES_DIR/mipmap-anydpi-v26" "$RES_DIR/drawable" "$RES_DIR/values"
if [ ! -f "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" ]; then
  cat > "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF
fi
if [ ! -f "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml" ]; then
  cat > "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF
fi
if [ ! -f "$RES_DIR/drawable/ic_launcher_foreground.xml" ]; then
  cat > "$RES_DIR/drawable/ic_launcher_foreground.xml" <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#202124" android:pathData="M0,0h108v108h-108z"/>
    <!-- 간단한 A 모양 -->
    <path android:fillColor="#3DDC84"
        android:pathData="M54,18 L78,90 L66,90 L60,72 L48,72 L42,90 L30,90 Z"/>
</vector>
EOF
fi
if ! grep -q 'name="ic_launcher_background"' "$RES_DIR/values/ic_launcher_background.xml" 2>/dev/null; then
  cat > "$RES_DIR/values/ic_launcher_background.xml" <<'EOF'
<resources>
    <color name="ic_launcher_background">#121212</color>
</resources>
EOF
fi

# 4) Manifest 정규화
# - package 속성 제거(Gradle namespace 사용)
# - application에 icon/roundIcon/label/allowBackup/supportsRtl/Theme 보장
# - MAIN/LAUNCHER Activity의 exported 보장
if [ -f "$MANIFEST" ]; then
  # package 속성 제거
  if grep -q 'package=' "$MANIFEST"; then
    sed -i 's/\s\+package="[^"]*"\s*/ /' "$MANIFEST"
  fi

  # application 태그 보강
  if ! grep -q 'android:icon=' "$MANIFEST"; then
    sed -i 's#<application#<application android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"#' "$MANIFEST"
  fi
  if ! grep -q 'android:label=' "$MANIFEST"; then
    sed -i 's#<application#<application android:label="@string/app_name"#' "$MANIFEST"
  fi
  if ! grep -q 'android:supportsRtl=' "$MANIFEST"; then
    sed -i 's#<application#<application android:supportsRtl="true"#' "$MANIFEST"
  fi
  if ! grep -q 'android:allowBackup=' "$MANIFEST"; then
    sed -i 's#<application#<application android:allowBackup="true"#' "$MANIFEST"
  fi
  if ! grep -q 'android:theme=' "$MANIFEST"; then
    sed -i 's#<application#<application android:theme="@style/Theme.MyApp"#' "$MANIFEST"
  fi

  # MAIN/LAUNCHER Activity exported 보장
  if grep -q 'android.intent.category.LAUNCHER' "$MANIFEST"; then
    # LAUNCHER를 가진 activity 블록에 exported="true" 주입(이미 있으면 유지)
    awk '
      BEGIN{inAct=0}
      /<activity/{inAct=1}
      inAct && /android.intent.category.LAUNCHER/ { need=1 }
      inAct && /<\/activity>/{ 
        if (need && $0 !~ /exported=/) print gensub(/<activity /,"<activity android:exported=\"true\" ","g",$0); 
        else print $0; 
        inAct=0; need=0; next 
      }
      { print $0 }
    ' "$MANIFEST" > "$MANIFEST.tmp" && mv "$MANIFEST.tmp" "$MANIFEST"
  fi
fi

# 5) app/build.gradle.kts 필수 의존성 확인
if [ -f "$APP_GRADLE" ]; then
  # core-ktx/appcompat/material/constraintlayout 가 없으면 추가
  grep -q 'androidx.core:core-ktx' "$APP_GRADLE" || \
    sed -i '/dependencies\s*{.*/a \ \ \ \ implementation("androidx.core:core-ktx:1.13.1")' "$APP_GRADLE"
  grep -q 'androidx.appcompat:appcompat' "$APP_GRADLE" || \
    sed -i '/dependencies\s*{.*/a \ \ \ \ implementation("androidx.appcompat:appcompat:1.7.0")' "$APP_GRADLE"
  grep -q 'com.google.android.material:material' "$APP_GRADLE" || \
    sed -i '/dependencies\s*{.*/a \ \ \ \ implementation("com.google.android.material:material:1.12.0")' "$APP_GRADLE"
  grep -q 'androidx.constraintlayout:constraintlayout' "$APP_GRADLE" || \
    sed -i '/dependencies\s*{.*/a \ \ \ \ implementation("androidx.constraintlayout:constraintlayout:2.1.4")' "$APP_GRADLE"
fi

echo "[preflight] 완료"
