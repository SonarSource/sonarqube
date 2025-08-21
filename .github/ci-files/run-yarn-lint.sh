#!/bin/bash

./gradlew yarn_lint-report-ci \
  --parallel --configure-on-demand \
  --console plain \
  -Pqa --profile
