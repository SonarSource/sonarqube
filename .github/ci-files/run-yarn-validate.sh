#!/bin/bash

set +e

./gradlew yarn_validate-ci \
  --parallel --configure-on-demand \
  --console plain \
  -Pqa --profile

gradle_return_code=$?

set -e

exit $gradle_return_code
