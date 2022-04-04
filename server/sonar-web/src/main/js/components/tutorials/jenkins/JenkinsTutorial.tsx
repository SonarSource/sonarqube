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
import { connect } from 'react-redux';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getAppState, getCurrentUserSetting, Store } from '../../../store/rootReducer';
import { setCurrentUserSetting } from '../../../store/users';
import {
  AlmSettingsInstance,
  isProjectBitbucketBindingResponse,
  isProjectGitHubBindingResponse,
  isProjectGitLabBindingResponse,
  ProjectAlmBindingResponse
} from '../../../types/alm-settings';
import JenkinsfileStep from './JenkinsfileStep';
import MultiBranchPipelineStep from './MultiBranchPipelineStep';
import PipelineStep from './PipelineStep';
import PreRequisitesStep from './PreRequisitesStep';
import WebhookStep from './WebhookStep';

export interface JenkinsTutorialProps {
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  component: T.Component;
  projectBinding: ProjectAlmBindingResponse;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
  skipPreReqs: boolean;
}

enum Steps {
  PreRequisites = 0,
  MultiBranchPipeline = 1,
  Webhook = 2,
  Jenkinsfile = 3
}

const USER_SETTING_SKIP_BITBUCKET_PREREQS = 'tutorials.jenkins.skipBitbucketPreReqs';

export function JenkinsTutorial(props: JenkinsTutorialProps) {
  const { almBinding, branchesEnabled, component, projectBinding, skipPreReqs } = props;
  const [step, setStep] = React.useState(
    skipPreReqs ? Steps.MultiBranchPipeline : Steps.PreRequisites
  );

  // Failsafe; should never happen.
  if (
    !isProjectBitbucketBindingResponse(projectBinding) &&
    !isProjectGitHubBindingResponse(projectBinding) &&
    !isProjectGitLabBindingResponse(projectBinding)
  ) {
    return (
      <Alert variant="error">{translate('onboarding.tutorial.with.jenkins.unsupported')}</Alert>
    );
  }

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h1 className="page-title">{translate('onboarding.tutorial.with.jenkins.title')}</h1>
      </div>

      <PreRequisitesStep
        alm={projectBinding.alm}
        branchesEnabled={branchesEnabled}
        onDone={() => setStep(Steps.MultiBranchPipeline)}
        onOpen={() => setStep(Steps.PreRequisites)}
        onChangeSkipNextTime={skip => {
          props.setCurrentUserSetting({
            key: USER_SETTING_SKIP_BITBUCKET_PREREQS,
            value: skip.toString()
          });
        }}
        open={step === Steps.PreRequisites}
        skipNextTime={skipPreReqs}
      />

      {branchesEnabled ? (
        <MultiBranchPipelineStep
          almBinding={almBinding}
          finished={step > Steps.MultiBranchPipeline}
          onDone={() => setStep(Steps.Webhook)}
          onOpen={() => setStep(Steps.MultiBranchPipeline)}
          open={step === Steps.MultiBranchPipeline}
          projectBinding={projectBinding}
        />
      ) : (
        <PipelineStep
          alm={projectBinding.alm}
          finished={step > Steps.MultiBranchPipeline}
          onDone={() => setStep(Steps.Webhook)}
          onOpen={() => setStep(Steps.MultiBranchPipeline)}
          open={step === Steps.MultiBranchPipeline}
        />
      )}

      <WebhookStep
        almBinding={almBinding}
        branchesEnabled={branchesEnabled}
        finished={step > Steps.Webhook}
        onDone={() => setStep(Steps.Jenkinsfile)}
        onOpen={() => setStep(Steps.Webhook)}
        open={step === Steps.Webhook}
        projectBinding={projectBinding}
      />

      <JenkinsfileStep component={component} open={step === Steps.Jenkinsfile} />
    </>
  );
}

const mapStateToProps = (
  state: Store
): Pick<JenkinsTutorialProps, 'branchesEnabled' | 'skipPreReqs'> => {
  return {
    branchesEnabled: Boolean(getAppState(state).branchesEnabled),
    skipPreReqs: getCurrentUserSetting(state, USER_SETTING_SKIP_BITBUCKET_PREREQS) === 'true'
  };
};

const mapDispatchToProps = { setCurrentUserSetting };

export default connect(mapStateToProps, mapDispatchToProps)(JenkinsTutorial);
