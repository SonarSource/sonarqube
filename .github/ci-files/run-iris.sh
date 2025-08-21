#!/bin/bash
set -euo pipefail

: "${ARTIFACTORY_USERNAME?}" "${ARTIFACTORY_ACCESS_TOKEN?}" "${ARTIFACTORY_URL?}"
: "${SONARQUBE_NEXT_URL?}" "${SONARQUBE_NEXT_IRIS_TOKEN?}"

# Hardcoded version of Iris to use until the issue with the api/issues/search endpoint is resolved.
# This version leverages the export findings endpoint.
VERSION="1.6.2.1310"
HTTP_CODE=$(\
  curl \
    --write-out '%{http_code}' \
    --location \
    --remote-name \
    --user "$ARTIFACTORY_USERNAME:$ARTIFACTORY_ACCESS_TOKEN" \
    "$ARTIFACTORY_URL/sonarsource-private-releases/com/sonarsource/iris/iris/$VERSION/iris-$VERSION-jar-with-dependencies.jar"\
)

if [ "$HTTP_CODE" != "200" ]; then
  echo "Download $VERSION failed -> $HTTP_CODE"
  exit 1
else
  echo "Downloaded $VERSION"
fi

java \
  -Diris.source.projectKey=org.sonarsource.sonarqube:sonarqube-private \
  -Diris.source.url="$SONARQUBE_NEXT_URL" \
  -Diris.source.token="$SONARQUBE_NEXT_IRIS_TOKEN" \
  -Diris.destination.projectKey=sonarqube \
  -Diris.destination.url="$SONARQUBE_NEXT_URL" \
  -Diris.destination.token="$SONARQUBE_NEXT_IRIS_TOKEN" \
  -Diris.dryrun=true \
  -jar iris-1.6.2.1310-jar-with-dependencies.jar
