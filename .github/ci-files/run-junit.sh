#!/bin/bash

set +e

./gradlew cleanTest test jacocoTestReport \
  --parallel --configure-on-demand \
  --console plain \
  --max-workers=3 \
  -Pqa --profile

gradle_return_code=$?

set -e

exit $gradle_return_code
