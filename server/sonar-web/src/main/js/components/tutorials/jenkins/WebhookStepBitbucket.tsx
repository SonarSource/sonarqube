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
import { FormattedMessage } from 'react-intl';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../../types/alm-settings';
import CodeSnippet from '../../common/CodeSnippet';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface WebhookStepBitbucketProps {
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  projectBinding: ProjectAlmBindingResponse;
}

function buildUrlSnippet(branchesEnabled: boolean, ownUrl = '***BITBUCKET_URL***') {
  return branchesEnabled
    ? `***JENKINS_SERVER_URL***/bitbucket-scmsource-hook/notify?server_url=${ownUrl}`
    : '***JENKINS_SERVER_URL***/job/***JENKINS_JOB_NAME***/build?token=***JENKINS_BUILD_TRIGGER_TOKEN***';
}

export default function WebhookStepBitbucket(props: WebhookStepBitbucketProps) {
  const { almBinding, branchesEnabled, projectBinding } = props;

  const linkUrl =
    almBinding &&
    almBinding.url &&
    `${almBinding.url}/plugins/servlet/webhooks/projects/${projectBinding.repository}/repos/${projectBinding.slug}/create`;

  return (
    <>
      <li>
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.step1.sentence')}
          id="onboarding.tutorial.with.jenkins.webhook.step1.sentence"
          values={{
            link: linkUrl ? (
              <a href={linkUrl} rel="noopener noreferrer" target="_blank">
                {translate('onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.link')}
              </a>
            ) : (
              translate('onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.link')
            )
          }}
        />
        <ul className="list-styled">
          <li>
            <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.step1.name" />
          </li>
          <li className="abs-width-600">
            <p>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.url" />
            </p>
            <CodeSnippet
              isOneLine={true}
              snippet={buildUrlSnippet(branchesEnabled, almBinding && almBinding.url)}
            />
            {branchesEnabled && (
              <Alert variant="info">
                {translate('onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.url.warning')}
              </Alert>
            )}
          </li>
        </ul>
      </li>
      <li>
        <SentenceWithHighlights
          highlightKeys={['events']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step2"
        />
        <ul className="list-styled">
          <li>
            <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step2.repo" />
          </li>
          {branchesEnabled && (
            <li>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step2.pr" />
            </li>
          )}
        </ul>
      </li>
      <li>
        <SentenceWithHighlights
          highlightKeys={['create']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.step3"
        />
      </li>
    </>
  );
}
