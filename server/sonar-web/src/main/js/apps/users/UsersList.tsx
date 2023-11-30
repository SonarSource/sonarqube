/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra" id="users-list">
        <thead>
          <tr>
            <th className="nowrap">{translate('users.user_name')}</th>
            <th className="nowrap">{translate('my_profile.scm_accounts')}</th>
            <th className="nowrap">{translate('users.last_connection')}</th>
            <th className="nowrap">
              {translate('users.last_sonarlint_connection')}
              <HelpTooltip
                className="sw-ml-1"
                overlay={translate('users.last_sonarlint_connection.help_text')}
              />
            </th>
            <th className="nowrap">{translate('my_profile.groups')}</th>
            <th className="nowrap">{translate('users.tokens')}</th>
            {(manageProvider === undefined || users.some((u) => !u.managed)) && (
              <th className="nowrap">{translate('actions')}</th>
            )}
          </tr>
        </thead>
        <tbody>
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
        </tbody>
      </table>
    </div>
  );
}
