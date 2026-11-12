#!/usr/bin/env bash
set -euo pipefail
if command -v android >/dev/null 2>&1; then
  android sdk install platforms/android-37.0 build-tools/36.0.0
else
  sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'
fi
