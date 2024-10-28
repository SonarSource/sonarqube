#!/bin/bash

function getProjectVersion() {
  local projectVersion
  # We will need to get it from package.json once in the new repo, something like: $(jq -r .version "package.json")
  projectVersion=$(grep "version=" gradle.properties | cut -d= -f2)
  [[ "${projectVersion//[^\.]}" == "." ]] && projectVersion="${projectVersion}.0"
  echo "$projectVersion"
}

VERSION="$(getProjectVersion)"
ARTIFACT_NAME="webapp-assets"
BUILD_NAME="sonarqube-$ARTIFACT_NAME"
UPLOADED_ZIP_NAME="$ARTIFACT_NAME-$VERSION.$BUILD_NUMBER.zip"
ARTIFACT_FILE_PATH="server/sonar-web/build/$ARTIFACT_NAME.zip"

PUBLISH_COMMON_PARAMS="build.number=$BUILD_NUMBER"
PUBLISH_COMMON_PARAMS="$PUBLISH_COMMON_PARAMS;vcs.revision=$GIT_SHA1"
PUBLISH_COMMON_PARAMS="$PUBLISH_COMMON_PARAMS;vcs.branch=$GITHUB_BRANCH"
PUBLISH_COMMON_PARAMS="$PUBLISH_COMMON_PARAMS;pr.number=$PULL_REQUEST"
PUBLISH_COMMON_PARAMS="$PUBLISH_COMMON_PARAMS;pr.branch.target=$GITHUB_BASE_BRANCH"

publish_to_artifactory()
{
  PUBLISH_URL=$1
  LOCAL_FILE=$2
  BUILD_NAME=$3
  ARTIFACTORY_DEPLOY_REPO=$4

  PARAMS="$PUBLISH_COMMON_PARAMS;build.name=$BUILD_NAME"
  API_CALL="$PUBLISH_URL;$PARAMS"

  echo "Publishing $LOCAL_FILE to ${ARTIFACTORY_DEPLOY_REPO}"
  echo "Publish params: $PARAMS"

  #Upload build artifact
  HTTP_CODE=$(curl -H "Authorization: Bearer $ARTIFACTORY_DEPLOY_PASSWORD" -s -o /dev/null -w %{http_code} -XPUT $API_CALL -T $LOCAL_FILE)
  if [ "$HTTP_CODE" != "201" ]; then
      echo "Cannot upload $LOCAL_FILE to $ARTIFACTORY_DEPLOY_REPO: HTTP return code $HTTP_CODE"
      exit 1
  else
      echo "$LOCAL_FILE uploaded to ${ARTIFACTORY_DEPLOY_REPO}"
  fi
}

create_build_in_artifactory()
{
  ZIP_API_CALL=$1
  PATH_TO_BUILD_JSON=$2
  ARTIFACTORY_DEPLOY_REPO=$3

  #Get MD5 and SHA1 values
  RESPONSE=$(curl -H "Authorization: Bearer $ARTIFACTORY_DEPLOY_PASSWORD" -s -XGET $ZIP_API_CALL)
  ZIP_MD5=$(echo $RESPONSE | jq .checksums.md5)
  ZIP_SHA1=$(echo $RESPONSE | jq .checksums.sha1)
  TIME_CREATED=$(echo $RESPONSE | jq .created)

  #Replace the values in template file
  sed -i -e "s/VERSION/$VERSION/g" $PATH_TO_BUILD_JSON
  sed -i -e "s/BUILD_NUMBER/$BUILD_NUMBER/g" $PATH_TO_BUILD_JSON
  sed -i -e "s/ZIP_SHA1/$ZIP_SHA1/g" $PATH_TO_BUILD_JSON
  sed -i -e "s/ZIP_MD5/$ZIP_MD5/g" $PATH_TO_BUILD_JSON
  sed -i -e "s/TIME_CREATED/$TIME_CREATED/g" $PATH_TO_BUILD_JSON
  sed -i -e "s/ARTIFACTORY_DEPLOY_REPO/$ARTIFACTORY_DEPLOY_REPO/g" $PATH_TO_BUILD_JSON

  echo "Creating build for $UPLOADED_ZIP_NAME in ${ARTIFACTORY_DEPLOY_REPO}"

  #Create a build in artifactory
  API_CALL="$ARTIFACTORY_URL/api/build"
  HTTP_CODE=$(curl -H "Content-Type: application/json" -H "Authorization: Bearer $ARTIFACTORY_DEPLOY_PASSWORD" -s -o /dev/null -w %{http_code} -XPUT $API_CALL --upload-file $PATH_TO_BUILD_JSON)
  if [ "$HTTP_CODE" != "204" ]; then
      echo "Cannot create build for $UPLOADED_ZIP_NAME in $ARTIFACTORY_DEPLOY_REPO: HTTP return code $HTTP_CODE"
      exit 1
  else
      echo "build for $UPLOADED_ZIP_NAME created in $ARTIFACTORY_DEPLOY_REPO"
  fi
}

PUT_URL="$ARTIFACTORY_URL/$ARTIFACTORY_DEPLOY_REPO_PRIVATE/com/sonarsource/sonarqube/$ARTIFACT_NAME/$VERSION.$BUILD_NUMBER/$UPLOADED_ZIP_NAME"
publish_to_artifactory "$PUT_URL" "$ARTIFACT_FILE_PATH" "$BUILD_NAME" "$ARTIFACTORY_DEPLOY_REPO_PRIVATE"

ZIP_GET_URL="$ARTIFACTORY_URL/api/storage/$ARTIFACTORY_DEPLOY_REPO_PRIVATE/com/sonarsource/sonarqube/$ARTIFACT_NAME/$VERSION.$BUILD_NUMBER/$UPLOADED_ZIP_NAME"
BUILD_JSON=.cirrus/$ARTIFACT_NAME-build.json

create_build_in_artifactory "$ZIP_GET_URL" "$BUILD_JSON" "$ARTIFACTORY_DEPLOY_REPO_PRIVATE"
