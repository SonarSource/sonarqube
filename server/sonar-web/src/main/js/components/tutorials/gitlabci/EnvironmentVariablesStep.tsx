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
import {
  BasicSeparator,
  ClipboardIconButton,
  ListItem,
  NumberedList,
  NumberedListItem,
  TutorialStep,
  UnorderedList,
} from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import { InlineSnippet } from '../components/InlineSnippet';
import TokenStepGenerator from '../components/TokenStepGenerator';

export interface EnvironmentVariablesStepProps {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
}

const pipelineDescriptionLinkLabel = translate(
  'onboarding.tutorial.with.gitlab_ci.variables.description.link',
);

export default function EnvironmentVariablesStep(props: EnvironmentVariablesStepProps) {
  const { baseUrl, component, currentUser } = props;

  const fieldValueTranslation = translate('onboarding.tutorial.env_variables');

  const renderForm = () => (
    <NumberedList>
      <NumberedListItem>
        {translate('onboarding.tutorial.with.gitlab_ci.variables.section.title')}

        <FormattedMessage
          defaultMessage={translate(
            'onboarding.tutorial.with.gitlab_ci.variables.section.description',
          )}
          id="onboarding.tutorial.with.gitlab_ci.variables.section.description"
          values={{
            /* This link will be added when the backend provides the project URL */
            link: <span className="sw-typo-semibold">{pipelineDescriptionLinkLabel}</span>,
          }}
        />

        <UnorderedList ticks className="sw-ml-10">
          <ListItem>
            <FormattedMessage
              id="onboarding.tutorial.with.gitlab_ci.variables.step1"
              values={{
                extra: (
                  <ClipboardIconButton copyValue="SONAR_TOKEN" className="sw-ml-1 sw-align-sub" />
                ),

                value: <InlineSnippet snippet="SONAR_TOKEN" />,
              }}
            />
          </ListItem>
          <ListItem>
            <TokenStepGenerator component={component} currentUser={currentUser} />
          </ListItem>
          <ListItem>
            <FormattedMessage
              defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.variables.step3')}
              id="onboarding.tutorial.with.gitlab_ci.variables.step3"
              values={{
                value: (
                  <span className="sw-typo-semibold">
                    {translate('onboarding.tutorial.with.gitlab_ci.variables.step3.value')}
                  </span>
                ),
              }}
            />
          </ListItem>
          <ListItem>
            <FormattedMessage
              defaultMessage={translate(
                'onboarding.tutorial.with.gitlab_ci.variables.section.step4',
              )}
              id="onboarding.tutorial.with.gitlab_ci.variables.section.step4"
              values={{
                value: (
                  <span className="sw-typo-semibold">
                    {translate('onboarding.tutorial.with.gitlab_ci.variables.section.step4.value')}
                  </span>
                ),
              }}
            />
          </ListItem>
        </UnorderedList>
        <BasicSeparator className="sw-my-6" />
      </NumberedListItem>
      <NumberedListItem>
        {translate('onboarding.tutorial.with.gitlab_ci.variables.section2.title')}

        <FormattedMessage
          defaultMessage={translate(
            'onboarding.tutorial.with.gitlab_ci.variables.section2.description',
          )}
          id="onboarding.tutorial.with.gitlab_ci.variables.section2.description"
          values={{
            /* This link will be added when the backend provides the project URL */
            link: <span className="sw-typo-semibold">{pipelineDescriptionLinkLabel}</span>,
          }}
        />

        <UnorderedList ticks className="sw-ml-10">
          <ListItem>
            <FormattedMessage
              defaultMessage={fieldValueTranslation}
              id="onboarding.tutorial.with.gitlab_ci.variables.step1"
              values={{
                extra: (
                  <ClipboardIconButton
                    copyValue="SONAR_HOST_URL"
                    className="sw-ml-1 sw-align-sub"
                  />
                ),
                value: <InlineSnippet snippet="SONAR_HOST_URL" />,
              }}
            />
          </ListItem>
          <ListItem>
            <FormattedMessage
              defaultMessage={fieldValueTranslation}
              id="onboarding.tutorial.with.gitlab_ci.variables.step2"
              values={{
                extra: <ClipboardIconButton copyValue={baseUrl} className="sw-ml-1 sw-align-sub" />,
                field: (
                  <span className="sw-typo-semibold">
                    {translate('onboarding.tutorial.env_variables.field')}
                  </span>
                ),
                value: <InlineSnippet snippet={baseUrl} />,
              }}
            />
          </ListItem>
          <ListItem>
            <FormattedMessage
              defaultMessage={translate('onboarding.tutorial.with.gitlab_ci.variables.step3')}
              id="onboarding.tutorial.with.gitlab_ci.variables.step3"
              values={{
                value: (
                  <span className="sw-typo-semibold">
                    {translate('onboarding.tutorial.with.gitlab_ci.variables.step3.value')}
                  </span>
                ),
              }}
            />
          </ListItem>
          <ListItem>
            <FormattedMessage
              defaultMessage={translate(
                'onboarding.tutorial.with.gitlab_ci.variables.section2.step4',
              )}
              id="onboarding.tutorial.with.gitlab_ci.variables.section2.step4"
              values={{
                value: (
                  <span className="sw-typo-semibold">
                    {translate('onboarding.tutorial.with.gitlab_ci.variables.section.step4.value')}
                  </span>
                ),
              }}
            />
          </ListItem>
        </UnorderedList>
      </NumberedListItem>
    </NumberedList>
  );

  return (
    <TutorialStep title={translate('onboarding.tutorial.with.gitlab_ci.variables.title')}>
      {renderForm()}
    </TutorialStep>
  );
}
