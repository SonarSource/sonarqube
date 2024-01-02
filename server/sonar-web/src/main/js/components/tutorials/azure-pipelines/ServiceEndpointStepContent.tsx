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
import { TokenType } from '../../../types/token';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import EditTokenModal from '../components/EditTokenModal';
import SentenceWithHighlights from '../components/SentenceWithHighlights';

export interface ServiceEndpointStepProps {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
}

export default function ServiceEndpointStepContent(props: ServiceEndpointStepProps) {
  const { baseUrl, component, currentUser } = props;

  const [isModalVisible, toggleModal] = React.useState(false);

  return (
    <>
      <ol className="list-styled">
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step1"
            highlightKeys={['menu']}
          />
        </li>
        <li>
          <SentenceWithHighlights
            translationKey="onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step2"
            highlightKeys={['type']}
          />
        </li>
        <li>
          <FormattedMessage
            defaultMessage={translate(
              'onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step3.sentence'
            )}
            id="onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step3.sentence"
            values={{
              url: <code className="rule">{baseUrl}</code>,
              button: <ClipboardIconButton copyValue={baseUrl} />,
            }}
          />
        </li>
        <li>
          <span>
            {translate('onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step4.sentence')}:
          </span>
          <Button className="spacer-left" onClick={() => toggleModal(true)}>
            {translate('onboarding.token.generate.long')}
          </Button>
        </li>
        <li>
          {translate('onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step5.sentence')}
        </li>
        <li>
          {translate('onboarding.tutorial.with.azure_pipelines.ServiceEndpoint.step6.sentence')}
        </li>
      </ol>

      {isModalVisible && (
        <EditTokenModal
          component={component}
          currentUser={currentUser}
          onClose={() => toggleModal(false)}
          preferredTokenType={TokenType.Global}
        />
      )}
    </>
  );
}
