#!/bin/bash

PUBLIC_WEBAPP_VERSION="$(cat gradle.properties | grep -oP 'webappVersion=\K(\S+)')"

ASSET_NAME="webapp-assets"

MAVEN_CENTRAL_FOLDER="https://repo.maven.apache.org/maven2/org/sonarsource/sonarqube/$ASSET_NAME/"
MAVEN_CENTRAL_FOLDER+=$PUBLIC_WEBAPP_VERSION

WEBAPP_ASSETS="$ASSET_NAME-$PUBLIC_WEBAPP_VERSION"

# We're testing the presence of the JAR's MD5 file on Maven Central as it's a small one to test for.
# If it's here, the .jar is here as well, so public builds from the sonarqube repository will work.
# This also means we have both artifacts (webapp-assets and webapp-assets-enterprise) on Repox in
# sonarsource-public-releases and sonarsource-private-releases, so builds from sonar-enterprise
# will work too.
JAR_MD5_FILE="$MAVEN_CENTRAL_FOLDER/$WEBAPP_ASSETS.jar.md5"

HTTP_CODE=$(curl -s -o /dev/null --head -w %{http_code} "$JAR_MD5_FILE")

if [ "$HTTP_CODE" != "200" ]; then
  LVL="WARN"

  if [[ "$GITHUB_BASE_REF" == "master" ]] || [[ "$GITHUB_BASE_REF" == "branch-"* ]]; then
    LVL="FAIL"
  fi

  echo
  echo "[$LVL] This PR cannot be merged to master or release branches: $WEBAPP_ASSETS is not (yet?) on Maven Central."
  echo
  echo "       This means that a build from the public sonarqube repository would fail."
  echo

  if [[ "$LVL" == "WARN" ]]; then
    echo "       The PR can still be merged to \"$GITHUB_BASE_REF\", but that branch won't be mergeable to master or release branches right away."
  fi

  echo "       You will first need to either create a release for $WEBAPP_ASSETS (from the webapp repository),"
  echo "       or change the webappVersion in gradle.properties to a released one."
  echo

  exit 1
else
  echo
  echo "[OK] The build step from the public sonarque repository can use $WEBAPP_ASSETS"
  echo "     from Maven Central, this PR can be merged to master or a release branch ('branch-xx')."
  echo

  exit 0
fi
