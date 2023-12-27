#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew cleanTest test jacocoTestReport \
  --parallel --configure-on-demand --console plain -Pqa
