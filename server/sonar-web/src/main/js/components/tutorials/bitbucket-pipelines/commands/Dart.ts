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

import { Arch, OSs } from '../../types';
import { SONAR_SCANNER_CLI_LATEST_VERSION, getScannerUrlSuffix } from '../../utils';
import { BuildToolExampleBuilder } from '../AnalysisCommand';

const dartExample: BuildToolExampleBuilder = ({ branchesEnabled, mainBranchName }) => {
  const scannerSuffix = getScannerUrlSuffix(OSs.Linux, Arch.X86_64);
  return `image: ghcr.io/cirruslabs/flutter:stable

definitions:
  steps:
    - step: &build-step
        name: Build the project, and run the SonarQube analysis
        script:
          - <commands to build your project>
          - export SONAR_SCANNER_VERSION=${SONAR_SCANNER_CLI_LATEST_VERSION}
          - mkdir $HOME/.sonar
          - curl -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-\${SONAR_SCANNER_VERSION}${scannerSuffix}.zip
          - unzip -o $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
          - export PATH="$PATH:$HOME/.sonar/sonar-scanner-\${SONAR_SCANNER_VERSION}${scannerSuffix}/bin"
          - sonar-scanner
  caches:
    sonar: ~/.sonar

clone:
  depth: full

pipelines:
  branches:
    '{${mainBranchName}}':
      - step: *build-step
${
  branchesEnabled
    ? `
  pull-requests:
    '**':
      - step: *build-step`
    : ''
}`;
};

export default dartExample;
