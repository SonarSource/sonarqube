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
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { stripTrailingSlash } from '../../../helpers/urls';
import {
  AlmKeys,
  AlmSettingsInstance,
  ProjectAlmBindingResponse,
} from '../../../types/alm-settings';
import CodeSnippet from '../../common/CodeSnippet';
import Link from '../../common/Link';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import { buildBitbucketCloudLink } from '../utils';

export interface WebhookStepBitbucketProps {
  alm: AlmKeys;
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

function buildUrlSnippet(
  branchesEnabled: boolean,
  isBitbucketcloud: boolean,
  ownUrl = '***BITBUCKET_URL***'
) {
  if (!branchesEnabled) {
    return '***JENKINS_SERVER_URL***/job/***JENKINS_JOB_NAME***/build?token=***JENKINS_BUILD_TRIGGER_TOKEN***';
  }
  return isBitbucketcloud
    ? '***JENKINS_SERVER_URL***/bitbucket-scmsource-hook/notify'
    : `***JENKINS_SERVER_URL***/bitbucket-scmsource-hook/notify?server_url=${ownUrl}`;
}

export default function WebhookStepBitbucket(props: WebhookStepBitbucketProps) {
  const { alm, almBinding, branchesEnabled, projectBinding } = props;

  const isBitbucketCloud = alm === AlmKeys.BitbucketCloud;

  let linkUrl;
  if (almBinding?.url && projectBinding) {
    if (isBitbucketCloud && projectBinding?.repository) {
      linkUrl = `${buildBitbucketCloudLink(
        almBinding,
        projectBinding
      )}/admin/addon/admin/bitbucket-webhooks/bb-webhooks-repo-admin`;
    } else if (projectBinding.slug) {
      linkUrl = `${stripTrailingSlash(almBinding.url)}/plugins/servlet/webhooks/projects/${
        projectBinding.repository
      }/repos/${projectBinding.slug}/create`;
    }
  }

  return (
    <>
      <li>
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.step1.sentence')}
          id="onboarding.tutorial.with.jenkins.webhook.step1.sentence"
          values={{
            link: linkUrl ? (
              <Link to={linkUrl} target="_blank">
                {translate('onboarding.tutorial.with.jenkins.webhook', alm, 'step1.link')}
              </Link>
            ) : (
              <strong>
                {translate('onboarding.tutorial.with.jenkins.webhook', alm, 'step1.link')}
              </strong>
            ),
          }}
        />
        <ul className="list-styled list-alpha">
          <li>
            <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.step1.name" />
          </li>
          <li className="abs-width-600">
            <p>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.url" />
            </p>
            <CodeSnippet
              isOneLine={true}
              snippet={buildUrlSnippet(
                branchesEnabled,
                isBitbucketCloud,
                almBinding && almBinding.url
              )}
            />
            {branchesEnabled && !isBitbucketCloud && (
              <Alert variant="info">
                {translate('onboarding.tutorial.with.jenkins.webhook.bitbucket.step1.url.warning')}
              </Alert>
            )}
          </li>
        </ul>
      </li>
      {isBitbucketCloud ? (
        <li>
          <SentenceWithHighlights
            highlightKeys={['triggers', 'option']}
            translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucketcloud.step2"
          />
          <ul className="list-styled list-alpha">
            <li>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucketcloud.step2.repo" />
            </li>
            {branchesEnabled && (
              <li>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucketcloud.step2.pr" />
              </li>
            )}
          </ul>
        </li>
      ) : (
        <li>
          <SentenceWithHighlights
            highlightKeys={['events']}
            translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step2"
          />
          <ul className="list-styled list-alpha">
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
      )}
      <li>
        {isBitbucketCloud ? (
          <SentenceWithHighlights
            highlightKeys={['save']}
            translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucketcloud.step3"
          />
        ) : (
          <SentenceWithHighlights
            highlightKeys={['create']}
            translationKey="onboarding.tutorial.with.jenkins.webhook.bitbucket.step3"
          />
        )}
      </li>
    </>
  );
}
