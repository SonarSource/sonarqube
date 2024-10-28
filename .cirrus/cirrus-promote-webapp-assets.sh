#!/bin/bash

if [ "$PULL_REQUEST" != "" ]; then
  PRIVATE_TARGET_REPO='sonarsource-private-dev'
  PUBLIC_TARGET_REPO='sonarsource-public-dev'
  STATUS='it-passed-pr'
# CirrusCI build on master branch
elif [[ "$CIRRUS_CI" == "true" ]] && [[ ("$GITHUB_BRANCH" == "master" || "$GITHUB_BRANCH" == "branch-"*) ]]; then
  PRIVATE_TARGET_REPO='sonarsource-private-builds'
  PUBLIC_TARGET_REPO='sonarsource-public-builds'
  STATUS='it-passed'
fi

if [ -n "${STATUS:-}" ]; then
  echo "Promoting build sonarqube-webapp-assets#$BUILD_NUMBER"
  OP_DATE=$(date +%Y%m%d%H%M%S)
  # TODO Need to add promotion for public repo see cirrus-promote.sh
  DATA_JSON="{ \"status\": \"$STATUS\", \"properties\": { \"release\" : [ \"$OP_DATE\" ]}, \"targetRepo\": \"$PRIVATE_TARGET_REPO\", \"copy\": false }"
  HTTP_CODE=$(curl -s -o /dev/null -w %{http_code} -H "Content-Type: application/json" -H "Authorization: Bearer ${ARTIFACTORY_PROMOTE_ACCESS_TOKEN}" -X POST "$ARTIFACTORY_URL/api/build/promote/sonarqube-webapp-assets/$BUILD_NUMBER" --data "$DATA_JSON")
  if [ "$HTTP_CODE" != "200" ]; then
    echo "Cannot promote build sonarqube-webapp-assets#$BUILD_NUMBER: HTTP return code $HTTP_CODE"
    exit 1
  else
    echo "Build sonarqube-webapp-assets#${BUILD_NUMBER} promoted to ${PRIVATE_TARGET_REPO}"
  fi
else
  echo 'No promotion for builds coming from a development branch'
fi
