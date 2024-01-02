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

import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { rawSizes } from '../../../app/theme';
import { Button, ButtonLink } from '../../../components/controls/buttons';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { translate } from '../../../helpers/l10n';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import Step from '../components/Step';
import WebhookStepBitbucket from './WebhookStepBitbucket';
import WebhookStepGithub from './WebhookStepGithub';
import WebhookStepGitLab from './WebhookStepGitLab';

export interface WebhookStepProps {
  alm: AlmKeys;
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  finished: boolean;
  onDone: () => void;
  onOpen: () => void;
  open: boolean;
  projectBinding?: ProjectAlmBindingResponse;
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
  const { alm, finished, open } = props;

  return (
    <Step
      finished={finished}
      onOpen={props.onOpen}
      open={open}
      renderForm={() => (
        <div className="boxed-group-inner">
          <p className="big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.intro.sentence')}
              id="onboarding.tutorial.with.jenkins.webhook.intro.sentence"
              values={{
                link: (
                  <ButtonLink onClick={props.onDone}>
                    {translate('onboarding.tutorial.with.jenkins.webhook.intro.link')}
                  </ButtonLink>
                ),
              }}
            />
          </p>
          <ol className="list-styled">{renderAlmSpecificInstructions(props)}</ol>
          <Button className="big-spacer-top" onClick={props.onDone}>
            {translate('continue')}
            <ChevronRightIcon size={rawSizes.baseFontSizeRaw} />
          </Button>
        </div>
      )}
      stepNumber={2}
      stepTitle={translate('onboarding.tutorial.with.jenkins.webhook', alm, 'title')}
    />
  );
}
