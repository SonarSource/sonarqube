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
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import AllSetStep from '../components/AllSetStep';
import Step from '../components/Step';
import YamlFileStep from '../components/YamlFileStep';
import AnalysisCommand from './AnalysisCommand';
import SecretStep from './SecretStep';

export enum Steps {
  CREATE_SECRET = 1,
  YAML = 2,
  ALL_SET = 3,
}

export interface GitHubActionTutorialProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  mainBranchName: string;
  projectBinding?: ProjectAlmBindingResponse;
  willRefreshAutomatically?: boolean;
}

export default function GitHubActionTutorial(props: GitHubActionTutorialProps) {
  const {
    almBinding,
    baseUrl,
    currentUser,
    component,
    projectBinding,
    mainBranchName,
    willRefreshAutomatically,
  } = props;

  const [step, setStep] = React.useState<Steps>(Steps.CREATE_SECRET);
  return (
    <>
      <Step
        finished={step > Steps.CREATE_SECRET}
        onOpen={() => setStep(Steps.CREATE_SECRET)}
        open={step === Steps.CREATE_SECRET}
        renderForm={() => (
          <SecretStep
            almBinding={almBinding}
            baseUrl={baseUrl}
            component={component}
            currentUser={currentUser}
            projectBinding={projectBinding}
            onDone={() => setStep(Steps.YAML)}
          />
        )}
        stepNumber={Steps.CREATE_SECRET}
        stepTitle={translate('onboarding.tutorial.with.github_action.create_secret.title')}
      />
      <Step
        finished={step > Steps.YAML}
        onOpen={() => setStep(Steps.YAML)}
        open={step === Steps.YAML}
        renderForm={() => (
          <YamlFileStep>
            {(buildTool) => (
              <AnalysisCommand
                buildTool={buildTool}
                mainBranchName={mainBranchName}
                component={component}
                onDone={() => setStep(Steps.ALL_SET)}
              />
            )}
          </YamlFileStep>
        )}
        stepNumber={Steps.YAML}
        stepTitle={translate('onboarding.tutorial.with.github_action.yaml.title')}
      />
      <AllSetStep
        alm={almBinding?.alm || AlmKeys.GitHub}
        open={step === Steps.ALL_SET}
        stepNumber={Steps.ALL_SET}
        willRefreshAutomatically={willRefreshAutomatically}
      />
    </>
  );
}
