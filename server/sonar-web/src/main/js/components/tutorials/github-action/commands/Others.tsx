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
import DefaultProjectKey from '../../components/DefaultProjectKey';
import CreateYmlFile from './CreateYmlFile';

export interface OthersProps {
  branchesEnabled?: boolean;
  component: T.Component;
}

const dotnetYamlTemplate = (branchesEnabled: boolean) => `name: Build
on:
  push:
    branches:
      - master # or the name of your main branch
${branchesEnabled ? '  pull_request:\n    types: [opened, synchronize, reopened]' : ''}
jobs:
  build:
    name: Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0
      - uses: docker://sonarsource/sonar-scanner-cli:latest
        env:
          GITHUB_TOKEN: \${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: \${{ secrets.SONAR_HOST_URL }}`;

export default function Others(props: OthersProps) {
  const { component, branchesEnabled } = props;
  return (
    <>
      <DefaultProjectKey component={component} />
      <CreateYmlFile yamlTemplate={dotnetYamlTemplate(!!branchesEnabled)} />
    </>
  );
}
