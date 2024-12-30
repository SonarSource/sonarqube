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

import * as React from 'react';
import { NumberedListItem } from '~design-system';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import CreateYmlFile from '../../components/CreateYmlFile';
import DefaultProjectKey from '../../components/DefaultProjectKey';
import RenderOptions from '../../components/RenderOptions';
import { OSs } from '../../types';
import { generateGitHubActionsYaml } from '../utils';
import MonorepoDocLinkFallback from './MonorepoDocLinkFallback';

export interface OthersProps {
  branchesEnabled?: boolean;
  buildSteps: string;
  component: Component;
  mainBranchName: string;
  monorepo?: boolean;
}

function otherYamlSteps(buildSteps: string, branchesEnabled: boolean) {
  let output =
    buildSteps +
    `
      - uses: sonarsource/sonarqube-scan-action@v4
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
      # - uses: sonarsource/sonarqube-quality-gate-action@v1
      #   timeout-minutes: 5
      #   env:
      #     SONAR_TOKEN: \${{ secrets.SONAR_TOKEN }}`;

  return output;
}

export default function Others(props: OthersProps) {
  const runsOn = {
    [OSs.Linux]: 'ubuntu-latest',
    [OSs.MacOS]: 'macos-latest',
    [OSs.Windows]: 'windows-latest',
  };

  const { component, branchesEnabled, mainBranchName, monorepo, buildSteps } = props;
  const [os, setOs] = React.useState<OSs>(OSs.Linux);

  return (
    <>
      <DefaultProjectKey component={component} monorepo={monorepo} />
      <NumberedListItem>
        <span>{translate('onboarding.build.other.os')}</span>
        <RenderOptions
          label={translate('onboarding.build.other.os')}
          checked={os}
          onCheck={(value: OSs) => setOs(value)}
          optionLabelKey="onboarding.build.other.os"
          options={Object.values(OSs)}
        />
      </NumberedListItem>
      {monorepo ? (
        <MonorepoDocLinkFallback />
      ) : (
        <CreateYmlFile
          yamlFileName=".github/workflows/build.yml"
          yamlTemplate={generateGitHubActionsYaml(
            mainBranchName,
            !!branchesEnabled,
            runsOn[os],
            otherYamlSteps(buildSteps, !!branchesEnabled),
          )}
        />
      )}
    </>
  );
}
