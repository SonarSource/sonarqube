/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Component } from '../../../../types/types';
import CreateYmlFile from '../../components/CreateYmlFile';
import FinishButton from '../../components/FinishButton';

export interface DotNetProps {
  branchesEnabled?: boolean;
  component: Component;
  onDone: () => void;
}

const dotnetYamlTemplate = (projectKey: string, branchesEnabled: boolean) => `name: Build
on:
  push:
    branches:
      - master # or the name of your main branch
${branchesEnabled ? '  pull_request:\n    types: [opened, synchronize, reopened]' : ''}
jobs:
  build:
    name: Build
    runs-on: windows-latest
    steps:
      - name: Set up JDK 11
        uses: actions/setup-java@v1
        with:
          java-version: 1.11
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis
      - name: Cache SonarQube packages
        uses: actions/cache@v1
        with:
          path: ~\\sonar\\cache
          key: \${{ runner.os }}-sonar
          restore-keys: \${{ runner.os }}-sonar
      - name: Cache SonarQube scanner
        id: cache-sonar-scanner
        uses: actions/cache@v1
        with:
          path: .\\.sonar\\scanner
          key: \${{ runner.os }}-sonar-scanner
          restore-keys: \${{ runner.os }}-sonar-scanner
      - name: Install SonarQube scanner
        if: steps.cache-sonar-scanner.outputs.cache-hit != 'true'
        shell: powershell
        run: |
          New-Item -Path .\\.sonar\\scanner -ItemType Directory
          dotnet tool update dotnet-sonarscanner --tool-path .\\.sonar\\scanner
      - name: Build and analyze
        env:
          GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}  # Needed to get PR information, if any
        shell: powershell
        run: |
          .\\.sonar\\scanner\\dotnet-sonarscanner begin /k:"${projectKey}" /d:sonar.login="\${{ secrets.SONAR_TOKEN }}" /d:sonar.host.url="\${{ secrets.SONAR_HOST_URL }}"
          dotnet build
          .\\.sonar\\scanner\\dotnet-sonarscanner end /d:sonar.login="\${{ secrets.SONAR_TOKEN }}"`;

export default function DotNet(props: DotNetProps) {
  const { component, branchesEnabled } = props;
  return (
    <>
      <CreateYmlFile
        yamlFileName=".github/workflows/build.yml"
        yamlTemplate={dotnetYamlTemplate(component.key, !!branchesEnabled)}
      />
      <FinishButton onClick={props.onDone} />
    </>
  );
}
