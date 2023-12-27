#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew yarn_lint-report-ci \
  --parallel --configure-on-demand --console plain -Pqa
