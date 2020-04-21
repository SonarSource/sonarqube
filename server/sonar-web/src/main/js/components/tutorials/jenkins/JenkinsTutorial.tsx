/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
  isBitbucketBindingDefinition,
  isProjectBitbucketBindingResponse
} from '../../../helpers/alm-settings';
import { AlmBindingDefinition, ProjectAlmBindingResponse } from '../../../types/alm-settings';
import BitbucketWebhookStep from './BitbucketWebhookStep';
import JenkinsfileStep from './JenkinsfileStep';
import MultiBranchPipelineStep from './MultiBranchPipelineStep';
import PreRequisitesStep from './PreRequisitesStep';

export interface JenkinsTutorialProps {
  almBinding?: AlmBindingDefinition;
  component: T.Component;
  projectBinding: ProjectAlmBindingResponse;
}

enum Steps {
  PreRequisites = 0,
  MultiBranchPipeline = 1,
  BitbucketWebhook = 2,
  Jenkinsfile = 3
}

export default function JenkinsTutorial(props: JenkinsTutorialProps) {
  const { almBinding, component, projectBinding } = props;
  const [step, setStep] = React.useState(Steps.PreRequisites);

  // Failsafe; should never happen.
  if (!isProjectBitbucketBindingResponse(projectBinding)) {
    return (
      <Alert variant="error">{translate('onboarding.tutorial.with.jenkins.only_bitbucket')}</Alert>
    );
  }

  return (
    <>
      <div className="page-header big-spacer-bottom">
        <h1 className="page-title">{translate('onboarding.tutorial.with.jenkins.title')}</h1>
      </div>

      <PreRequisitesStep
        onDone={() => setStep(Steps.MultiBranchPipeline)}
        onOpen={() => setStep(Steps.PreRequisites)}
        open={step === Steps.PreRequisites}
      />

      <MultiBranchPipelineStep
        finished={step > Steps.MultiBranchPipeline}
        onDone={() => setStep(Steps.BitbucketWebhook)}
        onOpen={() => setStep(Steps.MultiBranchPipeline)}
        open={step === Steps.MultiBranchPipeline}
        projectBinding={projectBinding}
      />

      <BitbucketWebhookStep
        almBinding={almBinding && isBitbucketBindingDefinition(almBinding) ? almBinding : undefined}
        finished={step > Steps.BitbucketWebhook}
        onDone={() => setStep(Steps.Jenkinsfile)}
        onOpen={() => setStep(Steps.BitbucketWebhook)}
        open={step === Steps.BitbucketWebhook}
        projectBinding={projectBinding}
      />

      <JenkinsfileStep component={component} open={step === Steps.Jenkinsfile} />
    </>
  );
}
