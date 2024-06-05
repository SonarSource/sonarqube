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

import { Modal } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { RestUserDetailed } from '../../../types/users';
import TokensForm from './TokensForm';

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

export default function TokensFormModal(props: Readonly<Props>) {
  const { user } = props;

  return (
    <Modal
      body={
        <TokensForm deleteConfirmation="inline" displayTokenTypeInput={false} login={user.login} />
      }
      headerTitle={
        <FormattedMessage
          defaultMessage={translate('users.user_X_tokens')}
          id="users.user_X_tokens"
          values={{ user: <em>{user.name}</em> }}
        />
      }
      isLarge
      onClose={props.onClose}
    />
  );
}
