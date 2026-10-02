#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-0.1.0}
JAR="dismal-desktop-${VERSION}.jar"

mvn -q -DskipTests package

jpackage \
  --name "DismalDesktop" \
  --input "target" \
  --main-jar "$JAR" \
  --type dmg
