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

import { FormattedMessage } from 'react-intl';
import { CodeSnippet, Link, ListItem, NumberedListItem, UnorderedList } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../../types/alm-settings';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import { buildGithubLink } from '../utils';

export interface WebhookStepGithubProps {
  almBinding?: AlmSettingsInstance;
  branchesEnabled: boolean;
  projectBinding?: ProjectAlmBindingResponse | null;
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
      <NumberedListItem>
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.step1.sentence')}
          id="onboarding.tutorial.with.jenkins.webhook.step1.sentence"
          values={{
            link: linkUrl ? (
              <Link to={linkUrl}>
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step1.link')}
              </Link>
            ) : (
              <strong className="sw-font-semibold">
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step1.link')}
              </strong>
            ),
          }}
        />
        <UnorderedList ticks className="sw-ml-12">
          <ListItem>
            <p>
              <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.github.step1.url" />
            </p>
            <CodeSnippet className="sw-p-4" isOneLine snippet={webhookUrl} />
          </ListItem>
        </UnorderedList>
      </NumberedListItem>
      <NumberedListItem>
        <SentenceWithHighlights
          highlightKeys={['events', 'option']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.github.step2"
        />
        <UnorderedList ticks className="sw-ml-12">
          <ListItem>
            <strong className="sw-font-semibold">
              {translate('onboarding.tutorial.with.jenkins.webhook.github.step2.repo')}
            </strong>
          </ListItem>
          {branchesEnabled && (
            <ListItem>
              <strong className="sw-font-semibold">
                {translate('onboarding.tutorial.with.jenkins.webhook.github.step2.pr')}
              </strong>
            </ListItem>
          )}
        </UnorderedList>
      </NumberedListItem>
      <NumberedListItem>
        <SentenceWithHighlights
          highlightKeys={['add_webhook']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.github.step3"
        />
      </NumberedListItem>
    </>
  );
}
