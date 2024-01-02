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
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import EditTokenModal from './EditTokenModal';

export interface TokenStepGeneratorProps {
  component: Component;
  currentUser: LoggedInUser;
}

export default function TokenStepGenerator(props: TokenStepGeneratorProps) {
  const { component, currentUser } = props;
  const [isModalVisible, toggleModal] = React.useState(false);

  const toggleTokenModal = () => toggleModal((visible) => !visible);
  const closeTokenModal = () => toggleModal(false);

  return (
    <>
      <li className="big-spacer-bottom">
        <FormattedMessage
          defaultMessage={translate('onboarding.tutorial.env_variables')}
          id="onboarding.tutorial.env_variables"
          values={{
            extra: (
              <Button className="spacer-left" onClick={toggleTokenModal}>
                {translate('onboarding.token.generate.long')}
              </Button>
            ),
            field: <strong>{translate('onboarding.tutorial.env_variables.field')}</strong>,
            value: translate('onboarding.tutorial.env_variables.token_generator.value'),
          }}
        />
      </li>
      {isModalVisible && (
        <EditTokenModal component={component} currentUser={currentUser} onClose={closeTokenModal} />
      )}
    </>
  );
}
