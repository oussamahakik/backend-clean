#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_21_HOME="/usr/lib/jvm/java-21-openjdk-amd64"

if [ -d "$JAVA_21_HOME" ]; then
  export JAVA_HOME="$JAVA_21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/caisse-gradle}"

cd "$PROJECT_DIR"
exec ./gradlew bootRun
