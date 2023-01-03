/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  isProjectGitLabBindingResponse,
  ProjectAlmBindingResponse
} from '../../../types/alm-settings';
import EnvironmentVariablesStep from './EnvironmentVariablesStep';
import ProjectKeyStep from './ProjectKeyStep';
import { GitlabBuildTools } from './types';
import YmlFileStep from './YmlFileStep';

export enum Steps {
  PROJECT_KEY,
  ENV_VARIABLES,
  YML
}

export interface GitLabCITutorialProps {
  baseUrl: string;
  component: T.Component;
  currentUser: T.LoggedInUser;
  projectBinding: ProjectAlmBindingResponse;
}

export default function GitLabCITutorial(props: GitLabCITutorialProps) {
  const { baseUrl, component, currentUser, projectBinding } = props;

  const [step, setStep] = React.useState(Steps.PROJECT_KEY);
  const [buildTool, setBuildTool] = React.useState<GitlabBuildTools | undefined>();

  // Failsafe; should never happen.
  if (!isProjectGitLabBindingResponse(projectBinding)) {
    return (
      <Alert variant="error">{translate('onboarding.tutorial.with.gitlab_ci.unsupported')}</Alert>
    );
  }

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h1 className="page-title">{translate('onboarding.tutorial.with.gitlab_ci.title')}</h1>
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

      <YmlFileStep buildTool={buildTool} open={step === Steps.YML} projectKey={component.key} />
    </>
  );
}
