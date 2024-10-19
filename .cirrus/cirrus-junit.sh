#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew cleanTest test jacocoTestReport \
  --parallel --max-workers=3 --configure-on-demand --console plain -Pqa
