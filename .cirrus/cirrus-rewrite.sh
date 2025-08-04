#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew rewriteDryRun --build-cache -Dorg.gradle.jvmargs=-Xmx8G
# https://docs.openrewrite.org/reference/faq#im-getting-javalangoutofmemoryerror-java-heap-space-when-running-openrewrite
