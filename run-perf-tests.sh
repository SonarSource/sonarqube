#!/bin/bash
set -euo pipefail

echo 'Run performance tests'

./gradlew --no-daemon --console plain \
  :tests:integrationTest \
  -Dcategory=ServerPerformance  \
  $*
