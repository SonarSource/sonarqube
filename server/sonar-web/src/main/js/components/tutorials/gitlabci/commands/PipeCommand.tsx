/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { CodeSnippet } from 'design-system';
import * as React from 'react';
import { BuildTools } from '../../types';

export interface PipeCommandProps {
  buildTool: Exclude<BuildTools, BuildTools.Cpp | BuildTools.ObjectiveC>;
  projectKey: string;
}

const BUILD_TOOL_SPECIFIC = {
  [BuildTools.Gradle]: {
    image: 'gradle:8.2.0-jdk17-jammy',
    script: () => 'gradle sonar',
  },
  [BuildTools.Maven]: {
    image: 'maven:3-eclipse-temurin-17',
    script: () => `
    - mvn verify sonar:sonar`,
  },
  [BuildTools.DotNet]: {
    image: 'mcr.microsoft.com/dotnet/sdk:7.0',
    script: (projectKey: string) => `
      - "apt-get update"
      - "apt-get install --yes --no-install-recommends openjdk-17-jre"
      - "dotnet tool install --global dotnet-sonarscanner"
      - "export PATH=\\"$PATH:$HOME/.dotnet/tools\\""
      - "dotnet sonarscanner begin /k:\\"${projectKey}\\" /d:sonar.token=\\"$SONAR_TOKEN\\" /d:\\"sonar.host.url=$SONAR_HOST_URL\\" "
      - "dotnet build"
      - "dotnet sonarscanner end /d:sonar.token=\\"$SONAR_TOKEN\\""`,
  },
  [BuildTools.Other]: {
    image: `
    name: sonarsource/sonar-scanner-cli:5.0
    entrypoint: [""]`,
    script: () => `
    - sonar-scanner`,
  },
};

export default function PipeCommand(props: PipeCommandProps) {
  const { projectKey, buildTool } = props;

  const { image, script } = BUILD_TOOL_SPECIFIC[buildTool];

  const command = `stages:
    - sonarqube-check
    - sonarqube-vulnerability-report

sonarqube-check:
  stage: sonarqube-check
  image: ${image}
  variables:
    SONAR_USER_HOME: "\${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
    GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
  cache:
    key: "\${CI_JOB_NAME}"
    paths:
      - .sonar/cache
  script: ${script(projectKey)}
  allow_failure: true
  only:
    - merge_requests
    - master
    - main
    - develop

sonarqube-vulnerability-report:
  stage: sonarqube-vulnerability-report
  script:
    - 'curl -u "\${SONAR_TOKEN}:" "\${SONAR_HOST_URL}/api/issues/gitlab_sast_export?projectKey=${projectKey}&branch=\${CI_COMMIT_BRANCH}&pullRequest=\${CI_MERGE_REQUEST_IID}" -o gl-sast-sonar-report.json'
  allow_failure: true
  only:
    - merge_requests
    - master
    - main
    - develop
  artifacts:
    expire_in: 1 day
    reports:
      sast: gl-sast-sonar-report.json
  dependencies:
    - sonarqube-check
`;

  return <CodeSnippet className="sw-p-6" snippet={command} language="yml" />;
}
