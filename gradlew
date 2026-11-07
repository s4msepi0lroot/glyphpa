#!/usr/bin/env sh
# Lightweight Gradle bootstrap for this sample project (Gradle 8.9).
# Uses an existing Gradle installation or downloads the official distribution.
set -eu
GRADLE_VERSION="8.9"
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
GRADLE_HOME_BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/tapglyph-distributions"
GRADLE_HOME="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$GRADLE_HOME_BASE"
  ZIP_FILE="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION-bin.zip"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP_FILE"
  unzip -q -o "$ZIP_FILE" -d "$GRADLE_HOME_BASE"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
