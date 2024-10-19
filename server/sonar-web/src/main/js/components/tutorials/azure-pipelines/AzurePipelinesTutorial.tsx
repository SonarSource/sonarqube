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
import { BasicSeparator, Title, TutorialStep, TutorialStepList } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { AlmKeys } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import AllSet from '../components/AllSet';
import { TutorialConfig } from '../types';
import BranchAnalysisStepContent from './BranchAnalysisStepContent';
import ExtensionInstallationStepContent from './ExtensionInstallationStepContent';
import ServiceEndpointStepContent from './ServiceEndpointStepContent';

export interface AzurePipelinesTutorialProps {
  alm?: AlmKeys;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  willRefreshAutomatically?: boolean;
}

export enum Steps {
  ExtensionInstallation = 'ExtensionInstallation',
  ServiceEndpoint = 'ServiceEndpoint',
  BranchAnalysis = 'BranchAnalysis',
}

export default function AzurePipelinesTutorial(props: AzurePipelinesTutorialProps) {
  const { alm, baseUrl, component, currentUser, willRefreshAutomatically } = props;

  const [config, setConfig] = React.useState<TutorialConfig>({});
  const [done, setDone] = React.useState<boolean>(false);

  React.useEffect(() => {
    setDone(Boolean(config.buildTool));
  }, [config.buildTool]);

  return (
    <>
      <Title>{translate('onboarding.tutorial.with.azure_pipelines.title')}</Title>

      <TutorialStepList className="sw-mb-10">
        <TutorialStep
          title={translate(
            `onboarding.tutorial.with.azure_pipelines.${Steps.ExtensionInstallation}.title`,
          )}
        >
          <ExtensionInstallationStepContent />
        </TutorialStep>

        <TutorialStep
          title={translate(
            `onboarding.tutorial.with.azure_pipelines.${Steps.ServiceEndpoint}.title`,
          )}
        >
          <ServiceEndpointStepContent
            baseUrl={baseUrl}
            component={component}
            currentUser={currentUser}
          />
        </TutorialStep>

        <TutorialStep
          title={translate(
            `onboarding.tutorial.with.azure_pipelines.${Steps.BranchAnalysis}.title`,
          )}
        >
          <BranchAnalysisStepContent config={config} setConfig={setConfig} component={component} />
        </TutorialStep>

        {done && (
          <>
            <BasicSeparator className="sw-my-10" />
            <AllSet
              alm={alm ?? AlmKeys.Azure}
              willRefreshAutomatically={willRefreshAutomatically}
            />
          </>
        )}
      </TutorialStepList>
    </>
  );
}
