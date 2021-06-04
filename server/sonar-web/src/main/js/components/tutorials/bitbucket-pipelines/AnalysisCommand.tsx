/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Dictionary } from 'lodash';
import * as React from 'react';
import { withAppState } from '../../hoc/withAppState';
import CreateYmlFile from '../components/CreateYmlFile';
import { BuildTools } from '../types';
import { PreambuleYaml } from './PreambuleYaml';

export interface AnalysisCommandProps {
  appState: T.AppState;
  buildTool?: BuildTools;
  component: T.Component;
}

const YamlTemplate: Dictionary<(branchesEnabled?: boolean, projectKey?: string) => string> = {
  [BuildTools.Gradle]: branchesEnabled => `image: openjdk:8

clone:
  depth: full
  
pipelines:
  branches:
    '{master}':
      - step:
          name: SonarQube analysis
          caches:
            - gradle
            - sonar
          script:
            - bash ./gradlew sonarqube
${
  branchesEnabled
    ? `
  pull-requests:
    '**':
      - step:
          name: SonarQube analysis
          caches:
            - gradle
            - sonar
          script:
            - bash ./gradlew sonarqube
`
    : ''
}
definitions:
  caches:
    sonar: ~/.sonar`,
  [BuildTools.Maven]: branchesEnabled => `image: maven:3.3.9

clone:
  depth: full
  
pipelines:
  branches:
    '{master}':
      - step:
          name: SonarQube analysis
          caches:
            - maven
            - sonar
          script:
            - mvn verify sonar:sonar
${
  branchesEnabled
    ? `
  pull-requests:
    '**':
      - step:
          name: SonarQube analysis
          caches:
            - maven
            - sonar
          script:
            - mvn verify sonar:sonar
`
    : ''
}  
definitions:
  caches:
    sonar: ~/.sonar`,
  [BuildTools.DotNet]: (
    branchesEnabled,
    projectKey
  ) => `image: mcr.microsoft.com/dotnet/core/sdk:latest
     
pipelines:
  branches:
    '{master}':
      - step:
          name: SonarQube analysis
          caches:
            - dotnetcore
            - sonar
          script:
            - apt-get update
            - apt-get install --yes openjdk-11-jre
            - dotnet tool install --global dotnet-sonarscanner
            - export PATH="$PATH:/root/.dotnet/tools"
            - dotnet sonarscanner begin /k:"${projectKey}" /d:"sonar.login=\${SONAR_TOKEN}"  /d:"sonar.host.url=\${SONAR_HOST_URL}"
            - dotnet build 
            - dotnet sonarscanner end /d:"sonar.login=\${SONAR_TOKEN}"
            ${
              branchesEnabled
                ? `
  pull-requests:
    '**':
      - step:
          name: SonarQube analysis
          caches:
            - dotnetcore
            - sonar
          script:
            - apt-get update
            - apt-get install --yes openjdk-11-jre
            - dotnet tool install --global dotnet-sonarscanner
            - export PATH="$PATH:/root/.dotnet/tools"
            - dotnet sonarscanner begin /k:"${projectKey}" /d:"sonar.login=\${SONAR_TOKEN}"  /d:"sonar.host.url=\${SONAR_HOST_URL}"
            - dotnet build 
            - dotnet sonarscanner end /d:"sonar.login=\${SONAR_TOKEN}"
                `
                : ''
            }  
definitions:
  caches:
    sonar: ~/.sonar`,
  [BuildTools.Other]: branchesEnabled => `image: maven:3.3.9

clone:
  depth: full

pipelines:
  branches:
    '{master}':
      - step:
          name: SonarQube analysis
          script:
            - pipe: sonarsource/sonarqube-scan:1.0.0
              variables:
                SONAR_HOST_URL: \${SONAR_HOST_URL} # Get the value from the repository/workspace variable.
                SONAR_TOKEN: \${SONAR_TOKEN} # Get the value from the repository/workspace variable. You shouldn't set secret in clear text here.
${
  branchesEnabled
    ? `
  pull-requests:
    '**':
      - step:
          name: SonarQube analysis
          script:
            - pipe: sonarsource/sonarqube-scan:1.0.0
              variables:
                SONAR_HOST_URL: \${SONAR_HOST_URL} # Get the value from the repository/workspace variable.
                SONAR_TOKEN: \${SONAR_TOKEN} # Get the value from the repository/workspace variable. You shouldn't set secret in clear text here.
`
    : ''
}  
definitions:
  caches:
    sonar: ~/.sonar`
};

export function AnalysisCommand(props: AnalysisCommandProps) {
  const {
    buildTool,
    component,
    appState: { branchesEnabled }
  } = props;

  if (!buildTool) {
    return null;
  }

  const yamlTemplate = YamlTemplate[buildTool](branchesEnabled, component.key);

  return (
    <>
      <PreambuleYaml buildTool={buildTool} component={component} />
      <CreateYmlFile yamlFileName="bitbucket-pipelines.yml" yamlTemplate={yamlTemplate} />
    </>
  );
}

export default withAppState(AnalysisCommand);
