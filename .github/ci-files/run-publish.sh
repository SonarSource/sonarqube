#!/bin/bash

ADDITIONAL_PARAMS=""
if [[ "$GITHUB_REF_NAME" == "master" ]] || [[ "$GITHUB_REF_NAME" == "branch-"* ]]; then
  ADDITIONAL_PARAMS=" -Prelease=true "
fi

./gradlew artifactoryPublish \
	-Pofficial=true \
	-PdeployCommunity=false \
	-DbuildNumber=$BUILD_NUMBER \
	--parallel \
	--console plain --build-cache \
	${ADDITIONAL_PARAMS}

./gradlew clean artifactoryPublish \
	-Pofficial=true \
	-PdeployCommunity=true \
	-DbuildNumber=$BUILD_NUMBER \
	--parallel \
	--console plain --build-cache \
	${ADDITIONAL_PARAMS}
