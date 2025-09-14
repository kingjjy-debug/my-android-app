#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# AndroidX 강제
grep -q '^android.useAndroidX=true$' "$ROOT/gradle.properties" 2>/dev/null || echo 'android.useAndroidX=true' >> "$ROOT/gradle.properties"
grep -q '^android.enableJetifier=true$' "$ROOT/gradle.properties" 2>/dev/null || echo 'android.enableJetifier=true' >> "$ROOT/gradle.properties"

# namespace 추출(없으면 기본값)
NS="$(sed -n 's/.*namespace *= *"\([^"]*\)".*/\1/p' "$ROOT/app/build.gradle.kts" | head -n1)"
[ -z "${NS:-}" ] && NS="com.example.myapplication"

# Manifest 정리
MAN="$ROOT/app/src/main/AndroidManifest.xml"
if [ -f "$MAN" ]; then
  sed -i 's/ *package="[^"]*"//g' "$MAN"
  sed -i 's/android:icon="@mipmap\/ic_launcher"/android:icon="@android:drawable\/ic_dialog_info"/g' "$MAN"
  sed -i 's/android:roundIcon="@mipmap\/ic_launcher_round"//g' "$MAN"
  if ! grep -q 'android:exported=' "$MAN"; then
    sed -i 's,<activity ,<activity android:exported="true" ,g' "$MAN"
  fi
fi

# MainActivity 패키지/임포트 보정
MA="$ROOT/app/src/main/java/${NS//.//}/MainActivity.kt"
if [ -f "$MA" ]; then
  sed -i "1s,^package .*$,package $NS," "$MA"
  grep -q 'import android.os.Bundle' "$MA" || sed -i '1i\import android.os.Bundle' "$MA"
  grep -q 'import androidx.appcompat.app.AppCompatActivity' "$MA" || sed -i '1i\import androidx.appcompat.app.AppCompatActivity' "$MA"
  grep -q 'import android.widget.*' "$MA" || sed -i '1i\import android.widget.*' "$MA"
fi

# 기본 레이아웃(누락 시 생성)
LAY="$ROOT/app/src/main/res/layout/activity_main.xml"
if [ ! -f "$LAY" ]; then
cat > "$LAY" <<XML
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:orientation="vertical"
  android:gravity="center">
  <TextView
    android:id="@+id/textView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Hello"/>
</LinearLayout>
XML
fi

# versionCode 자동 +1
BG="$ROOT/app/build.gradle.kts"
if [ -f "$BG" ]; then
  CUR=$(sed -n 's/.*versionCode *= *\([0-9][0-9]*\).*/\1/p' "$BG" | head -n1 || true)
  if [ -n "$CUR" ]; then
    NEW=$((CUR+1))
    sed -i "s/versionCode *= *$CUR/versionCode = $NEW/" "$BG"
  fi
fi

# strings.xml 최소 보정(누락 시 생성)
STR="$ROOT/app/src/main/res/values/strings.xml"
mkdir -p "$(dirname "$STR")"
if [ ! -f "$STR" ]; then
cat > "$STR" <<XML
<resources>
  <string name="app_name">MyApp</string>
</resources>
XML
fi

echo "✅ preflight-stabilize: 기본 안정화 완료"
