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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { translate } from '../../../helpers/l10n';
import { getCurrentUserSetting, Store } from '../../../store/rootReducer';
import { setCurrentUserSetting } from '../../../store/users';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse
} from '../../../types/alm-settings';
import { AppState, Component, CurrentUserSetting } from '../../../types/types';
import AllSetStep from '../components/AllSetStep';
import JenkinsfileStep from './JenkinsfileStep';
import MultiBranchPipelineStep from './MultiBranchPipelineStep';
import PipelineStep from './PipelineStep';
import PreRequisitesStep from './PreRequisitesStep';
import SelectAlmStep from './SelectAlmStep';
import WebhookStep from './WebhookStep';

export interface JenkinsTutorialProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  appState: AppState;
  component: Component;
  projectBinding?: ProjectAlmBindingResponse;
  setCurrentUserSetting: (setting: CurrentUserSetting) => void;
  skipPreReqs: boolean;
  willRefreshAutomatically?: boolean;
}

enum Steps {
  SelectAlm = 0,
  PreRequisites = 1,
  MultiBranchPipeline = 2,
  Webhook = 3,
  Jenkinsfile = 4,
  AllSet = 5
}

const USER_SETTING_SKIP_BITBUCKET_PREREQS = 'tutorials.jenkins.skipBitbucketPreReqs';

export function JenkinsTutorial(props: JenkinsTutorialProps) {
  const {
    almBinding,
    baseUrl,
    appState,
    component,
    projectBinding,
    skipPreReqs,
    willRefreshAutomatically
  } = props;
  const hasSelectAlmStep = projectBinding?.alm === undefined;
  const [alm, setAlm] = React.useState<AlmKeys | undefined>(projectBinding?.alm);

  let startStep;
  if (alm) {
    startStep = skipPreReqs ? Steps.MultiBranchPipeline : Steps.PreRequisites;
  } else {
    startStep = Steps.SelectAlm;
  }
  const [step, setStep] = React.useState(startStep);

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h1 className="page-title">{translate('onboarding.tutorial.with.jenkins.title')}</h1>
      </div>

      {hasSelectAlmStep && (
        <SelectAlmStep
          alm={alm}
          open={step === Steps.SelectAlm}
          onCheck={value => {
            setAlm(value);
            setStep(Steps.PreRequisites);
          }}
          onOpen={() => setStep(Steps.SelectAlm)}
        />
      )}

      {alm && (
        <>
          <PreRequisitesStep
            alm={alm}
            branchesEnabled={!!appState.branchesEnabled}
            finished={step > Steps.PreRequisites}
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

          {appState.branchesEnabled ? (
            <MultiBranchPipelineStep
              alm={alm}
              almBinding={almBinding}
              finished={step > Steps.MultiBranchPipeline}
              onDone={() => setStep(Steps.Webhook)}
              onOpen={() => setStep(Steps.MultiBranchPipeline)}
              open={step === Steps.MultiBranchPipeline}
              projectBinding={projectBinding}
            />
          ) : (
            <PipelineStep
              alm={alm}
              finished={step > Steps.MultiBranchPipeline}
              onDone={() => setStep(Steps.Webhook)}
              onOpen={() => setStep(Steps.MultiBranchPipeline)}
              open={step === Steps.MultiBranchPipeline}
            />
          )}

          <WebhookStep
            alm={alm}
            almBinding={almBinding}
            branchesEnabled={!!appState.branchesEnabled}
            finished={step > Steps.Webhook}
            onDone={() => setStep(Steps.Jenkinsfile)}
            onOpen={() => setStep(Steps.Webhook)}
            open={step === Steps.Webhook}
            projectBinding={projectBinding}
          />

          <JenkinsfileStep
            component={component}
            baseUrl={baseUrl}
            finished={step > Steps.Jenkinsfile}
            onDone={() => setStep(Steps.AllSet)}
            onOpen={() => setStep(Steps.Jenkinsfile)}
            open={step === Steps.Jenkinsfile}
          />

          <AllSetStep
            alm={alm}
            open={step === Steps.AllSet}
            stepNumber={4}
            willRefreshAutomatically={willRefreshAutomatically}
          />
        </>
      )}
    </>
  );
}

const mapStateToProps = (state: Store): Pick<JenkinsTutorialProps, 'skipPreReqs'> => {
  return {
    skipPreReqs: getCurrentUserSetting(state, USER_SETTING_SKIP_BITBUCKET_PREREQS) === 'true'
  };
};

const mapDispatchToProps = { setCurrentUserSetting };

export default connect(mapStateToProps, mapDispatchToProps)(withAppStateContext(JenkinsTutorial));
