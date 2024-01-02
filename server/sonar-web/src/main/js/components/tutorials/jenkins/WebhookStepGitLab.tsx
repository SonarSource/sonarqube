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
import { CodeSnippet, ListItem, NumberedListItem, UnorderedList } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import LabelActionPair from '../components/LabelActionPair';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface WebhookStepGitLabProps {
  branchesEnabled: boolean;
}

export default function WebhookStepGitLab({ branchesEnabled }: WebhookStepGitLabProps) {
  return (
    <>
      <NumberedListItem>
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.jenkins.webhook.step1.sentence')}
          id="onboarding.tutorial.with.jenkins.webhook.step1.sentence"
          values={{
            link: translate('onboarding.tutorial.with.jenkins.webhook.gitlab.step1.link'),
          }}
        />
        <UnorderedList ticks className="sw-ml-12">
          {branchesEnabled ? (
            <ListItem>
              <p>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.gitlab.step1.url_with_branches" />
              </p>
              <CodeSnippet
                className="sw-p-4"
                isOneLine
                snippet="***JENKINS_SERVER_URL***/gitlab-webhook/post"
              />
            </ListItem>
          ) : (
            <>
              <ListItem>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.gitlab.step1.url_no_branches" />
              </ListItem>
              <ListItem>
                <LabelActionPair translationKey="onboarding.tutorial.with.jenkins.webhook.gitlab.step1.secret_token" />
              </ListItem>
            </>
          )}
        </UnorderedList>
      </NumberedListItem>
      <NumberedListItem>
        <SentenceWithHighlights
          highlightKeys={['trigger']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.gitlab.step2"
        />
        <UnorderedList ticks className="sw-ml-12">
          <ListItem>
            <strong className="sw-font-semibold">
              {translate('onboarding.tutorial.with.jenkins.webhook.gitlab.step2.repo')}
            </strong>
          </ListItem>
          {branchesEnabled && (
            <ListItem>
              <strong className="sw-font-semibold">
                {translate('onboarding.tutorial.with.jenkins.webhook.gitlab.step2.mr')}
              </strong>
            </ListItem>
          )}
        </UnorderedList>
      </NumberedListItem>
      <NumberedListItem>
        <SentenceWithHighlights
          highlightKeys={['add_webhook']}
          translationKey="onboarding.tutorial.with.jenkins.webhook.gitlab.step3"
        />
      </NumberedListItem>
    </>
  );
}
