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
import { buildBitbucketCloudLink } from '../utils';

export interface RepositoryVariablesProps {
  almBinding?: AlmSettingsInstance;
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  onDone: () => void;
  projectBinding?: ProjectAlmBindingResponse;
}

export default function RepositoryVariables(props: RepositoryVariablesProps) {
  const { almBinding, baseUrl, component, currentUser, projectBinding } = props;
  return (
    <div className="boxed-group-inner">
      <p className="big-spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.with.bitbucket_pipelines.variables.intro')}
          id="onboarding.tutorial.with.bitbucket_pipelines.variables.intro"
          values={{
            repository_variables:
              almBinding?.url && projectBinding?.repository ? (
                <a
                  href={`${buildBitbucketCloudLink(
                    almBinding,
                    projectBinding
                  )}/admin/addon/admin/pipelines/repository-variables`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {translate('onboarding.tutorial.with.bitbucket_pipelines.variables.intro.link')}
                </a>
              ) : (
                <strong>
                  {translate('onboarding.tutorial.with.bitbucket_pipelines.variables.intro.link')}
                </strong>
              ),
          }}
        />
      </p>
      <ol className="list-styled">
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.bitbucket_pipelines.variables.name"
            highlightKeys={['name']}
          />
          <code className="rule little-spacer-left">SONAR_TOKEN</code>
          <ClipboardIconButton copyValue="SONAR_TOKEN" />
        </li>
        <TokenStepGenerator component={component} currentUser={currentUser} />
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.bitbucket_pipelines.variables.secured"
            highlightKeys={['secured']}
          />
        </li>
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.bitbucket_pipelines.variables.add"
            highlightKeys={['add']}
          />
        </li>
      </ol>

      <hr className="no-horizontal-margins" />

      <ol className="list-styled big-spacer-top big-spacer-bottom">
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.bitbucket_pipelines.variables.name"
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
            translationKey="onboarding.tutorial.with.bitbucket_pipelines.variables.add"
            highlightKeys={['add']}
          />
        </li>
      </ol>
      <Button onClick={props.onDone}>{translate('continue')}</Button>
    </div>
  );
}
