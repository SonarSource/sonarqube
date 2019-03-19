#!/bin/sh

./stop.sh

./gradlew build "$@"
