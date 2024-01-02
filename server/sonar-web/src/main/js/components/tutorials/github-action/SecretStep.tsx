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
import {
  BasicSeparator,
  ClipboardIconButton,
  NumberedList,
  NumberedListItem,
  StandoutLink,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { useProjectBindingQuery } from '../../../queries/devops-integration';
import { AlmSettingsInstance } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import { InlineSnippet } from '../components/InlineSnippet';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import TokenStepGenerator from '../components/TokenStepGenerator';
import { buildGithubLink } from '../utils';

export interface SecretStepProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
}

export default function SecretStep(props: SecretStepProps) {
  const { almBinding, baseUrl, component, currentUser } = props;
  const { data: projectBinding } = useProjectBindingQuery(component.key);

  return (
    <>
      <FormattedMessage
        defaultMessage={translate('onboarding.tutorial.with.github_action.secret.intro')}
        id="onboarding.tutorial.with.github_action.secret.intro"
        values={{
          settings_secret:
            almBinding && projectBinding ? (
              <StandoutLink
                to={`${buildGithubLink(almBinding, projectBinding)}/settings/secrets`}
                target="_blank"
                rel="noopener noreferrer"
              >
                {translate('onboarding.tutorial.with.github_action.secret.intro.link')}
              </StandoutLink>
            ) : (
              <span className="sw-body-sm-highlight">
                {translate('onboarding.tutorial.with.github_action.secret.intro.link')}
              </span>
            ),
        }}
      />
      <NumberedList>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.new"
            highlightKeys={['new_secret']}
          />
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.name"
            highlightKeys={['name']}
          />
          <InlineSnippet snippet="SONAR_TOKEN" className="sw-ml-1" />
          <ClipboardIconButton copyValue="SONAR_TOKEN" className="sw-ml-2 sw-align-sub" />
        </NumberedListItem>
        <NumberedListItem>
          <TokenStepGenerator component={component} currentUser={currentUser} />
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.add"
            highlightKeys={['add_secret']}
          />
        </NumberedListItem>
      </NumberedList>
      <BasicSeparator className="sw-my-6" />
      <NumberedList>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.new"
            highlightKeys={['new_secret']}
          />
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.name"
            highlightKeys={['name']}
          />
          <InlineSnippet snippet="SONAR_HOST_URL" className="sw-ml-1" />
          <ClipboardIconButton copyValue="SONAR_HOST_URL" className="sw-ml-2 sw-align-sub" />
        </NumberedListItem>
        <NumberedListItem>
          <FormattedMessage
            defaultMessage={translate('onboarding.tutorial.env_variables')}
            id="onboarding.tutorial.env_variables"
            values={{
              extra: <ClipboardIconButton copyValue={baseUrl} className="sw-ml-1 sw-align-sub" />,
              field: (
                <span className="sw-body-sm-highlight">
                  {translate('onboarding.tutorial.env_variables.field')}
                </span>
              ),
              value: <InlineSnippet snippet={baseUrl} className="sw-ml-1" />,
            }}
          />
        </NumberedListItem>
        <NumberedListItem>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.add"
            highlightKeys={['add_secret']}
          />
        </NumberedListItem>
      </NumberedList>
    </>
  );
}
