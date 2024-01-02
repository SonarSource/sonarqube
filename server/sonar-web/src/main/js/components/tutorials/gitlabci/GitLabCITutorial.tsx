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
import { AlmKeys } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import AllSetStep from '../components/AllSetStep';
import { BuildTools } from '../types';
import EnvironmentVariablesStep from './EnvironmentVariablesStep';
import ProjectKeyStep from './ProjectKeyStep';
import YmlFileStep from './YmlFileStep';

export enum Steps {
  PROJECT_KEY,
  ENV_VARIABLES,
  YML,
  ALL_SET,
}

export interface GitLabCITutorialProps {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  mainBranchName: string;
  willRefreshAutomatically?: boolean;
}

export default function GitLabCITutorial(props: GitLabCITutorialProps) {
  const { baseUrl, component, currentUser, willRefreshAutomatically, mainBranchName } = props;

  const [step, setStep] = React.useState(Steps.PROJECT_KEY);
  const [buildTool, setBuildTool] = React.useState<BuildTools>();

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h2 className="page-title">{translate('onboarding.tutorial.with.gitlab_ci.title')}</h2>
      </div>

      <ProjectKeyStep
        buildTool={buildTool}
        component={component}
        finished={step > Steps.PROJECT_KEY}
        onDone={() => setStep(Steps.ENV_VARIABLES)}
        onOpen={() => setStep(Steps.PROJECT_KEY)}
        open={step === Steps.PROJECT_KEY}
        setBuildTool={setBuildTool}
      />

      <EnvironmentVariablesStep
        baseUrl={baseUrl}
        component={component}
        currentUser={currentUser}
        finished={step > Steps.ENV_VARIABLES}
        onDone={() => setStep(Steps.YML)}
        onOpen={() => setStep(Steps.ENV_VARIABLES)}
        open={step === Steps.ENV_VARIABLES}
      />

      <YmlFileStep
        buildTool={buildTool}
        finished={step > Steps.YML}
        mainBranchName={mainBranchName}
        onDone={() => setStep(Steps.ALL_SET)}
        onOpen={() => setStep(Steps.YML)}
        open={step === Steps.YML}
        projectKey={component.key}
      />

      <AllSetStep
        alm={AlmKeys.GitLab}
        open={step === Steps.ALL_SET}
        stepNumber={4}
        willRefreshAutomatically={willRefreshAutomatically}
      />
    </>
  );
}
