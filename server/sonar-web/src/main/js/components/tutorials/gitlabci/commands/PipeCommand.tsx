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
import * as React from 'react';
import CodeSnippet from '../../../common/CodeSnippet';
import { CompilationInfo } from '../../components/CompilationInfo';
import { BuildTools } from '../../types';

export interface PipeCommandProps {
  branchesEnabled?: boolean;
  buildTool: BuildTools;
  mainBranchName: string;
  projectKey: string;
}

const BUILD_TOOL_SPECIFIC = {
  [BuildTools.Gradle]: { image: 'gradle:jre11-slim', script: () => 'gradle sonar' },
  [BuildTools.Maven]: {
    image: 'maven:3.6.3-jdk-11',
    script: (projectKey: string) => `
    - mvn verify sonar:sonar -Dsonar.projectKey=${projectKey}`,
  },
  [BuildTools.DotNet]: {
    image: 'mcr.microsoft.com/dotnet/core/sdk:latest',
    script: (projectKey: string) => `
      - "apt-get update"
      - "apt-get install --yes openjdk-11-jre"
      - "dotnet tool install --global dotnet-sonarscanner"
      - "export PATH=\\"$PATH:$HOME/.dotnet/tools\\""
      - "dotnet sonarscanner begin /k:\\"${projectKey}\\" /d:sonar.login=\\"$SONAR_TOKEN\\" /d:\\"sonar.host.url=$SONAR_HOST_URL\\" "
      - "dotnet build"
      - "dotnet sonarscanner end /d:sonar.login=\\"$SONAR_TOKEN\\""`,
  },
  [BuildTools.Other]: {
    image: `
    name: sonarsource/sonar-scanner-cli:latest
    entrypoint: [""]`,
    script: () => `
    - sonar-scanner`,
  },
};

export default function PipeCommand(props: PipeCommandProps) {
  const { projectKey, branchesEnabled, buildTool, mainBranchName } = props;
  let command: string;
  if (buildTool === BuildTools.CFamily) {
    command = `image: <image ready for your build toolchain>

cache:
  paths:
    - .sonar

stages:
  - download
  - build
  - scan

download:
  stage: download
  script:
      - mkdir -p .sonar
      - curl -sSLo build-wrapper-linux-x86.zip  $SONAR_HOST_URL/static/cpp/build-wrapper-linux-x86.zip
      - unzip -o build-wrapper-linux-x86.zip -d .sonar

build:
  stage: build
  script:
      - .sonar/build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir .sonar/bw-output <your clean build command>

sonarqube-check:
  stage: scan
  script: 
    - curl -sSLo sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.6.2.2472-linux.zip
    - unzip -o sonar-scanner.zip -d .sonar
    - .sonar/sonar-scanner-4.6.2.2472-linux/bin/sonar-scanner -Dsonar.cfamily.build-wrapper-output=.sonar/bw-output
  allow_failure: true`;
  } else {
    const onlyBlock = branchesEnabled
      ? `- merge_requests
    - ${mainBranchName}
    - develop`
      : `- ${mainBranchName}`;

    const { image, script } = BUILD_TOOL_SPECIFIC[buildTool];

    command = `sonarqube-check:
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
    ${onlyBlock}
`;
  }
  return (
    <>
      <CodeSnippet snippet={command} />
      {buildTool === BuildTools.CFamily && <CompilationInfo />}
    </>
  );
}
