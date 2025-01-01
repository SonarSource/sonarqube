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

export function generateGitHubActionsYaml(
  mainBranchName: string,
  branchesEnabled: boolean,
  runsOn: string,
  steps: string,
  additionalConfig?: string,
) {
  return `name: Build

on:
  push:
    branches:
      - ${mainBranchName}
${branchesEnabled ? '  pull_request:\n    types: [opened, synchronize, reopened]' : ''}

jobs:
  build:
    name: Build and analyze
    runs-on: ${runsOn}
    ${additionalConfig ?? ''}
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Shallow clones should be disabled for a better relevancy of analysis${steps}`;
}
