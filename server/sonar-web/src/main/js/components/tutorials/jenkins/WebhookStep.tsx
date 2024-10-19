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
/* eslint-disable react/no-unused-prop-types */

import { NumberedList, TutorialStep } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import WebhookStepBitbucket from './WebhookStepBitbucket';
import WebhookStepGitLab from './WebhookStepGitLab';
import WebhookStepGithub from './WebhookStepGithub';

export interface WebhookStepProps {
  alm: AlmKeys;
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  projectBinding?: ProjectAlmBindingResponse | null;
}

function renderAlmSpecificInstructions(props: WebhookStepProps) {
  const { alm, almBinding, branchesEnabled, projectBinding } = props;

  switch (alm) {
    case AlmKeys.BitbucketCloud:
    case AlmKeys.BitbucketServer:
      return (
        <WebhookStepBitbucket
          alm={alm}
          almBinding={almBinding}
          branchesEnabled={branchesEnabled}
          projectBinding={projectBinding}
        />
      );

    case AlmKeys.GitHub:
      return (
        <WebhookStepGithub
          almBinding={almBinding}
          branchesEnabled={branchesEnabled}
          projectBinding={projectBinding}
        />
      );

    case AlmKeys.GitLab:
      return <WebhookStepGitLab branchesEnabled={branchesEnabled} />;

    default:
      return null;
  }
}

export default function WebhookStep(props: WebhookStepProps) {
  const { alm } = props;

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.jenkins.webhook', alm, 'title')}>
      <p className="sw-mb-4">
        {translate('onboarding.tutorial.with.jenkins.webhook.intro.sentence')}
      </p>
      <NumberedList>{renderAlmSpecificInstructions(props)}</NumberedList>
    </TutorialStep>
  );
}
