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
export default function cFamilyExample(branchesEnabled: boolean, mainBranchName: string) {
  return `image: <image ready for your build toolchain>

definitions:
  steps:
    - step: &build-step
        name: Build the project, and run the SonarQube analysis
        script:
          - export SONAR_SCANNER_VERSION=5.0.1.3006
          - mkdir $HOME/.sonar
          - curl -sSLo $HOME/.sonar/build-wrapper-linux-x86.zip \${SONAR_HOST_URL}/static/cpp/build-wrapper-linux-x86.zip
          - unzip -o $HOME/.sonar/build-wrapper-linux-x86.zip -d $HOME/.sonar/
          - curl -sSLo $HOME/.sonar/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-\${SONAR_SCANNER_VERSION}-linux.zip
          - unzip -o $HOME/.sonar/sonar-scanner.zip -d $HOME/.sonar/
          - export PATH="$PATH:$HOME/.sonar/sonar-scanner-\${SONAR_SCANNER_VERSION}-linux/bin"
          - <any step required before running your build, like ./configure>
          - $HOME/.sonar/build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw-output <your clean build command>
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
}
