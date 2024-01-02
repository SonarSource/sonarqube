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
import { Button } from '../../../components/controls/buttons';
import { ClipboardIconButton } from '../../../components/controls/clipboard';
import { translate } from '../../../helpers/l10n';
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../../types/alm-settings';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import SentenceWithHighlights from '../components/SentenceWithHighlights';
import TokenStepGenerator from '../components/TokenStepGenerator';
import { buildGithubLink } from '../utils';

export interface SecretStepProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  projectBinding?: ProjectAlmBindingResponse;
  onDone: () => void;
}

export default function SecretStep(props: SecretStepProps) {
  const { almBinding, baseUrl, component, currentUser, projectBinding } = props;

  return (
    <div className="boxed-group-inner">
      <p className="big-spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.github_action.secret.intro')}
          id="onboarding.tutorial.with.github_action.secret.intro"
          values={{
            settings_secret:
              almBinding && projectBinding ? (
                <a
                  href={`${buildGithubLink(almBinding, projectBinding)}/settings/secrets`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {translate('onboarding.tutorial.with.github_action.secret.intro.link')}
                </a>
              ) : (
                <strong>
                  {translate('onboarding.tutorial.with.github_action.secret.intro.link')}
                </strong>
              ),
          }}
        />
      </p>
      <ol className="list-styled">
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.new"
            highlightKeys={['new_secret']}
          />
        </li>
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.name"
            highlightKeys={['name']}
          />
          <code className="rule little-spacer-left">SONAR_TOKEN</code>
          <ClipboardIconButton copyValue="SONAR_TOKEN" />
        </li>
        <TokenStepGenerator component={component} currentUser={currentUser} />
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.add"
            highlightKeys={['add_secret']}
          />
        </li>
      </ol>

      <hr className="no-horizontal-margins" />

      <ol className="list-styled big-spacer-top big-spacer-bottom">
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.new"
            highlightKeys={['new_secret']}
          />
        </li>
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.name"
            highlightKeys={['name']}
          />

          <code className="rule little-spacer-left">SONAR_HOST_URL</code>
          <ClipboardIconButton copyValue="SONAR_HOST_URL" />
        </li>
        <li className="big-spacer-bottom">
          <FormattedMessage
            defaultMessage={translate('onboarding.tutorial.env_variables')}
            id="onboarding.tutorial.env_variables"
            values={{
              extra: <ClipboardIconButton copyValue={baseUrl} />,
              field: <strong>{translate('onboarding.tutorial.env_variables.field')}</strong>,
              value: <code className="rule">{baseUrl}</code>,
            }}
          />
        </li>
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.github_action.secret.add"
            highlightKeys={['add_secret']}
          />
        </li>
      </ol>
      <Button onClick={props.onDone}>{translate('continue')}</Button>
    </div>
  );
}
