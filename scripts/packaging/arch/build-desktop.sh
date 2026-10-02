#!/usr/bin/env bash
set -euo pipefail

API_URL="$1"
APP_VERSION="${APP_VERSION:-1.0.0}"

./mvnw -f apps/desktop/pom.xml -DskipTests package

OUT_DIR="apps/desktop/target/installer"
mkdir -p "$OUT_DIR"

# Build app-image with jpackage
jpackage \
  --type app-image \
  --name "Dismal Desktop" \
  --app-version "${APP_VERSION}" \
  --input "apps/desktop/target" \
  --main-jar "dismal-desktop-0.1.0.jar" \
  --main-class "org.springframework.boot.loader.launch.JarLauncher" \
  --java-options "-Ddismal.api.url=${API_URL} -Dloader.main=com.dismal.desktop.DismalDesktopApp" \
  --dest "$OUT_DIR"

# Package into Arch pkg
WORK_DIR="$OUT_DIR/arch-pkg"
mkdir -p "$WORK_DIR"

TAR_PATH="$WORK_DIR/app-image.tar.gz"
rm -f "$TAR_PATH"

tar -czf "$TAR_PATH" -C "$OUT_DIR" "Dismal Desktop"

cp scripts/packaging/arch/PKGBUILD "$WORK_DIR/PKGBUILD"

pushd "$WORK_DIR" >/dev/null
makepkg -f
popd >/dev/null

mv "$WORK_DIR"/*.pkg.tar.zst "$OUT_DIR/"

echo "Package ready at $OUT_DIR"
