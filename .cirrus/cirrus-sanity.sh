#!/bin/bash
set -euo pipefail
source .cirrus/cirrus-env
./gradlew rewriteDryRun -Dorg.gradle.jvmargs=-Xmx8G
