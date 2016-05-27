#!/bin/bash
#

set -euo pipefail

#
# Evaluate a Maven expression
#

function maven_expression() {
  mvn help:evaluate -Dexpression=$1 | grep -v '^\[\|Download\w\+\:'
}

ARTIFACTID="`maven_expression project.artifactId`"
VERSION="`maven_expression project.version`"
BINTRAY_REPO=Distribution
BINTRAY_ACCOUNT=sonarsource

if [[ "$VERSION" =~ "-build" ]] || [[ "$VERSION" =~ "-SNAPSHOT" ]]; then   
  echo "This is a dev build, not releasing"
  exit 0
else
  echo "About to release sonarqube"    
fi

cd sonar-application

GROUPID="`maven_expression project.groupId`"
ARTIFACTID="`maven_expression project.artifactId`"
echo "$GROUPID $ARTIFACTID $VERSION"
GROUPIDPATH=`echo $GROUPID | sed 's/\./\//g'`

echo "Uploading sonarqube-$VERSION.zip to $BINTRAY_ACCOUNT/$BINTRAY_REPO"
cd target 

#zip file

HTTP_CODE=`curl --write-out %{http_code} -T sonarqube-$VERSION.zip -u$BINTRAY_USER:$BINTRAY_TOKEN https://api.bintray.com/content/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/sonarqube/`

echo $HTTP_CODE

if [[ "$HTTP_CODE" =~ "201" ]]; then   
  echo "Uploaded to bintray"
  echo "https://bintray.com/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/view#files"
else
  echo "Upload to bintray failed -> $HTTP_CODE"
  exit -1
fi

#md5 file

HTTP_CODE=`curl --write-out %{http_code} -T sonarqube-$VERSION.zip.md5 -u$BINTRAY_USER:$BINTRAY_TOKEN https://api.bintray.com/content/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/sonarqube/`

echo $HTTP_CODE

if [[ "$HTTP_CODE" =~ "201" ]]; then   
  echo "Uploaded to bintray"
  echo "https://bintray.com/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/view#files"
else
  echo "Upload to bintray failed -> $HTTP_CODE"
  exit -1
fi

#sha file

HTTP_CODE=`curl --write-out %{http_code} -T sonarqube-$VERSION.zip.sha -u$BINTRAY_USER:$BINTRAY_TOKEN https://api.bintray.com/content/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/sonarqube/`

echo $HTTP_CODE

if [[ "$HTTP_CODE" =~ "201" ]]; then   
  echo "Uploaded to bintray"
  echo "https://bintray.com/$BINTRAY_ACCOUNT/$BINTRAY_REPO/SonarQube/$VERSION/view#files"
else
  echo "Upload to bintray failed -> $HTTP_CODE"
  exit -1
fi

 