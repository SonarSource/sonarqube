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
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import AllSetStep from '../components/AllSetStep';
import JenkinsfileStep from './JenkinsfileStep';
import MultiBranchPipelineStep from './MultiBranchPipelineStep';
import PipelineStep from './PipelineStep';
import PreRequisitesStep from './PreRequisitesStep';
import SelectAlmStep from './SelectAlmStep';
import WebhookStep from './WebhookStep';

export interface JenkinsTutorialProps extends WithAvailableFeaturesProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  projectBinding?: ProjectAlmBindingResponse;
  willRefreshAutomatically?: boolean;
}

enum Steps {
  SelectAlm = 0,
  PreRequisites = 1,
  MultiBranchPipeline = 2,
  Webhook = 3,
  Jenkinsfile = 4,
  AllSet = 5,
}

export function JenkinsTutorial(props: JenkinsTutorialProps) {
  const { almBinding, baseUrl, component, projectBinding, willRefreshAutomatically } = props;
  const hasSelectAlmStep = projectBinding?.alm === undefined;
  const branchSupportEnabled = props.hasFeature(Feature.BranchSupport);
  const [alm, setAlm] = React.useState<AlmKeys | undefined>(projectBinding?.alm);
  const [step, setStep] = React.useState(alm ? Steps.PreRequisites : Steps.SelectAlm);

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h2 className="page-title">{translate('onboarding.tutorial.with.jenkins.title')}</h2>
      </div>

      {hasSelectAlmStep && (
        <SelectAlmStep
          alm={alm}
          open={step === Steps.SelectAlm}
          onCheck={(value) => {
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
            branchesEnabled={branchSupportEnabled}
            finished={step > Steps.PreRequisites}
            onDone={() => setStep(Steps.MultiBranchPipeline)}
            onOpen={() => setStep(Steps.PreRequisites)}
            open={step === Steps.PreRequisites}
          />

          {branchSupportEnabled ? (
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
            branchesEnabled={branchSupportEnabled}
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

export default withAvailableFeatures(JenkinsTutorial);
