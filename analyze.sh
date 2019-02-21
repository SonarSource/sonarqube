#!/bin/sh

sonar-scanner \
  -Dsonar.projectKey=sonar-ux-tests_sonarqube \
  -Dsonar.organization=sonar-ux-tests \
  -Dsonar.sources=./server/sonar-main/ \
  -Dsonar.host.url=https://wad.eu.ngrok.io \
  -Dsonar.java.binaries=./build/ \
  -Dsonar.login=39f60b4885cf76cdf51eab4521218edada8455f3
