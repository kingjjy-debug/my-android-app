#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
~/myapp/tools/gen-code.sh
~/myapp/tools/build-apk.sh main
