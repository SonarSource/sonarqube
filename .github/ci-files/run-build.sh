#!/bin/bash
set -euo pipefail

ADDITIONAL_PARAMS=""
if [[ "$GITHUB_REF_NAME" == "master" ]] || [[ "$GITHUB_REF_NAME" == "branch-"* ]]; then
  ADDITIONAL_PARAMS=" -Prelease=true "
fi

./gradlew build \
	-Pofficial=true \
	-PignoreLicenseFailures=false \
  -DbuildNumber=$BUILD_NUMBER \
  -x test \
  --parallel \
  --console plain --build-cache --profile \
  ${ADDITIONAL_PARAMS}
