/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { BuildTools } from '../../types';
import { GitlabBuildTools } from '../types';

export interface PipeCommandProps {
  branchesEnabled?: boolean;
  buildTool: GitlabBuildTools;
  projectKey: string;
}

const BUILD_TOOL_SPECIFIC = {
  [BuildTools.Gradle]: { image: 'gradle:jre11-slim', script: () => 'gradle sonarqube' },
  [BuildTools.Maven]: {
    image: 'maven:3.6.3-jdk-11',
    script: () => `
    - mvn verify sonar:sonar`
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
      - "dotnet sonarscanner end /d:sonar.login=\\"$SONAR_TOKEN\\""`
  },
  [BuildTools.Other]: {
    image: `
    name: sonarsource/sonar-scanner-cli:latest
    entrypoint: [""]`,
    script: () => `
    - sonar-scanner`
  }
};

export default function PipeCommand({ projectKey, branchesEnabled, buildTool }: PipeCommandProps) {
  const onlyBlock = branchesEnabled
    ? `- merge_requests
    - master
    - develop`
    : '- master # or the name of your main branch';

  const { image, script } = BUILD_TOOL_SPECIFIC[buildTool];

  const command = `sonarqube-check:
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

  return <CodeSnippet snippet={command} />;
}
