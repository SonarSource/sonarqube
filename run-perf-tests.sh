#!/bin/bash
set -euo pipefail

echo 'Run performance tests'

./gradlew --no-daemon --console plain -i \
  :tests:integrationTest \
  -Dcategory=ServerPerformance  \
  $*
