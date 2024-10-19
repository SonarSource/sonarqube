#!/bin/bash
set -euo pipefail

source .cirrus/cirrus-env

./gradlew build \
	-DbuildNumber="$BUILD_NUMBER" \
	-x test \
	--parallel --console plain --build-cache

pushToCirrusCiCache() {
  cd "${1}"
  zipfile=$(ls -- *.zip)
  echo "Uploading $zipfile to CirrusCI cache"
  curl -s -X POST --data-binary @"$zipfile" "http://$CIRRUS_HTTP_CACHE_HOST/$zipfile"
  echo "$zipfile successfully uploaded to CirrusCI cache"
}

export -f pushToCirrusCiCache
pushToCirrusCiCache sonar-application/build/distributions
