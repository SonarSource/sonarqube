#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew yarn_validate-ci \
  --parallel --configure-on-demand --console plain -Pqa
