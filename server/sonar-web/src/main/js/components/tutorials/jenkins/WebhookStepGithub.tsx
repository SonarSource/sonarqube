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
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../../types/alm-settings';
import CodeSnippet from '../../common/CodeSnippet';
import Link from '../../common/Link';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import { buildGithubLink } from '../utils';

export interface WebhookStepGithubProps {
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

export default function WebhookStepGithub(props: WebhookStepGithubProps) {
  const { almBinding, branchesEnabled, projectBinding } = props;

  const linkUrl =
    almBinding && projectBinding && `${buildGithubLink(almBinding, projectBinding)}/settings/hooks`;

  const webhookUrl = branchesEnabled
    ? '***JENKINS_SERVER_URL***/github-webhook/'
    : '***JENKINS_SERVER_URL***/job/***JENKINS_JOB_NAME***/build?token=***JENKINS_BUILD_TRIGGER_TOKEN***';

  return (
    <>
      <li>
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.step1.sentence')}
          id="onboarding.tutorial.with.jenkins.webhook.step1.sentence"
          values={{
            link: linkUrl ? (
              <Link to={linkUrl} target="_blank">
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step1.link')}
              </Link>
            ) : (
              <strong>
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step1.link')}
              </strong>
            ),
          }}
        />
        <ul className="list-styled">
          <li className="abs-width-600">
            <p>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.github.step1.url" />
            </p>
            <CodeSnippet isOneLine={true} snippet={webhookUrl} />
          </li>
        </ul>
      </li>
      <li>
        <SentenceWithHighlights
          highlightKeys={['events', 'option']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.github.step2"
        />
        <ul className="list-styled">
          <li>
            <strong>
              {translate('onboarding.tutorial.with.jenkins.webhook.github.step2.repo')}
            </strong>
          </li>
          {branchesEnabled && (
            <li>
              <strong>
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step2.pr')}
              </strong>
            </li>
          )}
        </ul>
      </li>
      <li>
        <SentenceWithHighlights
          highlightKeys={['add_webhook']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.github.step3"
        />
      </li>
    </>
  );
}
