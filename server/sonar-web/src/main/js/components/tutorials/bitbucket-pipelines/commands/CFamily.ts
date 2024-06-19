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
import { AutoConfig, BuildTools, OSs } from '../../types';
import {
  SONAR_SCANNER_CLI_LATEST_VERSION,
  getBuildWrapperExecutableLinux,
  getBuildWrapperFolderLinux,
  getScannerUrlSuffix,
} from '../../utils';
import { BuildToolExampleBuilder } from '../AnalysisCommand';
import othersExample from './Others';

const cFamilyExample: BuildToolExampleBuilder = ({
  config,
  arch,
  branchesEnabled,
  mainBranchName,
}) => {
  if (config.buildTool === BuildTools.Cpp && config.autoConfig === AutoConfig.Automatic) {
    return othersExample({ config, branchesEnabled, mainBranchName });
  }
  const buildWrapperExecutable = getBuildWrapperExecutableLinux(arch);
  const buildWrapperFolder = getBuildWrapperFolderLinux(arch);
  const scannerSuffix = getScannerUrlSuffix(OSs.Linux, arch);
  return `image: <image ready for your build toolchain>

definitions:
  steps:
    - step: &build-step
        name: Build the project, and run the SonarQube analysis
        script:
          - export SONAR_SCANNER_VERSION=${SONAR_SCANNER_CLI_LATEST_VERSION}
          - mkdir $HOME/.sonar
          - curl -sSLo $HOME/.sonar/${buildWrapperFolder}.zip \${SONAR_HOST_URL}/static/cpp/${buildWrapperFolder}.zip
          - unzip -o $HOME/.sonar/${buildWrapperFolder}.zip -d $HOME/.sonar/
          - curl -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-\${SONAR_SCANNER_VERSION}${scannerSuffix}.zip
          - unzip -o $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
          - export PATH="$PATH:$HOME/.sonar/sonar-scanner-\${SONAR_SCANNER_VERSION}${scannerSuffix}/bin"
          - <any step required before running your build, like ./configure>
          - $HOME/.sonar/${buildWrapperFolder}/${buildWrapperExecutable} --out-dir bw-output <your clean build command>
          - sonar-scanner -Dsonar.cfamily.compile-commands=bw-output/compile_commands.json  
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

export default cFamilyExample;
