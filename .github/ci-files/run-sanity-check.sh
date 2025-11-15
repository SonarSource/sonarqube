#!/bin/bash

set +e

./gradlew rewriteDryRun -Dorg.gradle.jvmargs=-Xmx8G \
  --configure-on-demand \
  --console plain \
  --max-workers=1 \
  -Pqa --profile

gradle_return_code=$?

set -e

exit $gradle_return_code
