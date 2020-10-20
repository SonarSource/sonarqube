#!/bin/bash

./stop.sh

if [ "$BUILD_NUMBER" == "" ]; then
	export BUILD_NUMBER=$((1 + RANDOM % 1000000))
	echo "Build $BUILD_NUMBER"
fi

TZ=UTC ./gradlew "-DbuildNumber=$BUILD_NUMBER" build $*
