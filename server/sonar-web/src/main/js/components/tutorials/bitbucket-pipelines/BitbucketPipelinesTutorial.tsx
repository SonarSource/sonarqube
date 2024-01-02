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
import FinishButton from '../components/FinishButton';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import Step from '../components/Step';
import YamlFileStep from '../components/YamlFileStep';
import { BuildTools, TutorialModes } from '../types';
import AnalysisCommand from './AnalysisCommand';
import RepositoryVariables from './RepositoryVariables';

export enum Steps {
  REPOSITORY_VARIABLES = 1,
  YAML = 2,
  ALL_SET = 3,
}

export interface BitbucketPipelinesTutorialProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  mainBranchName: string;
  projectBinding?: ProjectAlmBindingResponse;
  willRefreshAutomatically?: boolean;
}

export default function BitbucketPipelinesTutorial(props: BitbucketPipelinesTutorialProps) {
  const {
    almBinding,
    baseUrl,
    currentUser,
    component,
    projectBinding,
    willRefreshAutomatically,
    mainBranchName,
  } = props;

  const [step, setStep] = React.useState<Steps>(Steps.REPOSITORY_VARIABLES);
  return (
    <>
      <Step
        finished={step > Steps.REPOSITORY_VARIABLES}
        onOpen={() => setStep(Steps.REPOSITORY_VARIABLES)}
        open={step === Steps.REPOSITORY_VARIABLES}
        renderForm={() => (
          <RepositoryVariables
            almBinding={almBinding}
            baseUrl={baseUrl}
            component={component}
            currentUser={currentUser}
            onDone={() => setStep(Steps.YAML)}
            projectBinding={projectBinding}
          />
        )}
        stepNumber={Steps.REPOSITORY_VARIABLES}
        stepTitle={translate('onboarding.tutorial.with.bitbucket_pipelines.create_secret.title')}
      />
      <Step
        finished={step > Steps.YAML}
        onOpen={() => setStep(Steps.YAML)}
        open={step === Steps.YAML}
        renderForm={() => (
          <YamlFileStep>
            {(buildTool) => (
              <>
                {buildTool === BuildTools.CFamily && (
                  <GithubCFamilyExampleRepositories
                    className="big-spacer-top"
                    ci={TutorialModes.BitbucketPipelines}
                  />
                )}
                <AnalysisCommand
                  buildTool={buildTool}
                  component={component}
                  mainBranchName={mainBranchName}
                />
                <FinishButton onClick={() => setStep(Steps.ALL_SET)} />
              </>
            )}
          </YamlFileStep>
        )}
        stepNumber={Steps.YAML}
        stepTitle={translate('onboarding.tutorial.with.bitbucket_pipelines.yaml.title')}
      />
      <AllSetStep
        alm={almBinding?.alm || AlmKeys.BitbucketCloud}
        open={step === Steps.ALL_SET}
        stepNumber={Steps.ALL_SET}
        willRefreshAutomatically={willRefreshAutomatically}
      />
    </>
  );
}
