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

import { BuildToolExampleBuilder } from '../AnalysisCommand';

const othersExample: BuildToolExampleBuilder = ({ branchesEnabled, mainBranchName }) => {
  return `
definitions:
  steps:
    - step: &build-step
        name: SonarQube analysis
        script:
          - pipe: sonarsource/sonarqube-scan:3.0.2
            variables:
              SONAR_HOST_URL: \${SONAR_HOST_URL} # Get the value from the repository/workspace variable.
              SONAR_TOKEN: \${SONAR_TOKEN} # Get the value from the repository/workspace variable. You shouldn't set secret in clear text here.
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
      - step: *build-step
`
    : ''
}`;
};

export default othersExample;
