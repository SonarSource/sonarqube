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
import DefaultProjectKey from '../../components/DefaultProjectKey';
import FinishButton from '../../components/FinishButton';

export interface OthersProps {
  branchesEnabled?: boolean;
  component: Component;
  onDone: () => void;
}

const yamlTemplate = (branchesEnabled: boolean) => {
  let output = `name: Build
on:
  push:
    branches:
      - master # or the name of your main branch`;

  if (branchesEnabled) {
    output += `
  pull_request:
    types: [opened, synchronize, reopened]`;
  }

  output += `
jobs:
  build:
    name: Build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
        with:
          fetch-depth: 0
      - uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: \${{ secrets.SONAR_HOST_URL }}
      # If you wish to fail your job when the Quality Gate is red, uncomment the
      # following lines. This would typically be used to fail a deployment.`;

  if (branchesEnabled) {
    output += `
      # We do not recommend to use this in a pull request. Prefer using pull request
      # decoration instead.`;
  }

  output += `
      # - uses: sonarsource/sonarqube-quality-gate-action@master
      #   timeout-minutes: 5
      #   env:
      #     SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}`;

  return output;
};

export default function Others(props: OthersProps) {
  const { component, branchesEnabled } = props;
  return (
    <>
      <DefaultProjectKey component={component} />
      <CreateYmlFile
        yamlFileName=".github/workflows/build.yml"
        yamlTemplate={yamlTemplate(!!branchesEnabled)}
      />
      <FinishButton onClick={props.onDone} />
    </>
  );
}
