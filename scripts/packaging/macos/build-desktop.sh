#!/usr/bin/env bash
set -euo pipefail

API_URL="$1"
APP_VERSION="${APP_VERSION:-1.0.0}"

./mvnw -f apps/desktop/pom.xml -DskipTests package

OUT_DIR="apps/desktop/target/installer"
mkdir -p "$OUT_DIR"

jpackage \
  --type dmg \
  --name "Dismal Desktop" \
  --app-version "${APP_VERSION}" \
  --input "apps/desktop/target" \
  --main-jar "dismal-desktop-0.1.0.jar" \
  --main-class "org.springframework.boot.loader.launch.JarLauncher" \
  --java-options "-Ddismal.api.url=${API_URL} -Dloader.main=com.dismal.desktop.DismalDesktopApp" \
  --dest "$OUT_DIR"

echo "Installer ready at $OUT_DIR"
