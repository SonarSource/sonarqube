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

export default function cFamilyExample(branchesEnabled: boolean) {
  return `image: <image ready for your build toolchain>

clone:
  depth: full

pipelines:
  branches:
    '{master}': # or the name of your main branch
      - step:
          name: Download and install the build wrapper, build the project
          script:
            - mkdir $HOME/.sonar
            - curl -sSLo $HOME/.sonar/build-wrapper-linux-x86.zip \${SONAR_HOST_URL}/static/cpp/build-wrapper-linux-x86.zip
            - unzip -o $HOME/.sonar/build-wrapper-linux-x86.zip -d $HOME/.sonar/
            - $HOME/.sonar/build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw-output <your clean build command>
            - pipe: sonarsource/sonarqube-scan:1.0.0
              variables:
                EXTRA_ARGS: -Dsonar.cfamily.build-wrapper-output=bw-output
                SONAR_HOST_URL: \${SONAR_HOST_URL}
                SONAR_TOKEN: \${SONAR_TOKEN}
${
  branchesEnabled
    ? `
  pull-requests:
    '**':
      - step:
          name: Download and install the build wrapper, build the project
          script:
            - mkdir $HOME/.sonar
            - curl -sSLo $HOME/.sonar/build-wrapper-linux-x86.zip \${SONAR_HOST_URL}/static/cpp/build-wrapper-linux-x86.zip
            - unzip -o $HOME/.sonar/build-wrapper-linux-x86.zip -d $HOME/.sonar/
            - $HOME/.sonar/build-wrapper-linux-x86/build-wrapper-linux-x86-64 --out-dir bw-output <your clean build command>
            - pipe: sonarsource/sonarqube-scan:1.0.0
              variables:
                EXTRA_ARGS: -Dsonar.cfamily.build-wrapper-output=bw-output
                SONAR_HOST_URL: \${SONAR_HOST_URL}
                SONAR_TOKEN: \${SONAR_TOKEN}`
    : ''
}
definitions:
  caches:
    sonar: ~/.sonar`;
}
