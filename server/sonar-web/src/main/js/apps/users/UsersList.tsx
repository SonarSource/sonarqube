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
import { ActionCell, ContentCell, HelperHintIcon, Table, TableRow } from 'design-system';
import * as React from 'react';
import HelpTooltip from '../../components/controls/HelpTooltip';
import { translate } from '../../helpers/l10n';
import { IdentityProvider, Provider } from '../../types/types';
import { RestUserDetailed } from '../../types/users';
import UserListItem from './components/UserListItem';

interface Props {
  identityProviders: IdentityProvider[];
  users: RestUserDetailed[];
  manageProvider: Provider | undefined;
}

export default function UsersList({ identityProviders, users, manageProvider }: Props) {
  const header = (
    <TableRow>
      <ContentCell>{translate('users.user_name')}</ContentCell>
      <ContentCell>{translate('my_profile.scm_accounts')}</ContentCell>
      <ContentCell>{translate('users.last_connection')}</ContentCell>
      <ContentCell>
        {translate('users.last_sonarlint_connection')}
        <HelpTooltip overlay={translate('users.last_sonarlint_connection.help_text')}>
          <HelperHintIcon />
        </HelpTooltip>
      </ContentCell>
      <ContentCell>{translate('my_profile.groups')}</ContentCell>
      <ContentCell>{translate('users.tokens')}</ContentCell>
      {(manageProvider === undefined || users.some((u) => !u.managed)) && (
        <ActionCell>{translate('actions')}</ActionCell>
      )}
    </TableRow>
  );

  return (
    <Table columnCount={7} header={header} id="users-list">
      {users.map((user) => (
        <UserListItem
          identityProvider={identityProviders.find(
            (provider) => user.externalProvider === provider.key,
          )}
          key={user.login}
          user={user}
          manageProvider={manageProvider}
        />
      ))}
    </Table>
  );
}
