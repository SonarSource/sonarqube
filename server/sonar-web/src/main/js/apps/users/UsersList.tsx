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
import { translate } from '../../helpers/l10n';
import { IdentityProvider } from '../../types/types';
import { User } from '../../types/users';
import UserListItem from './components/UserListItem';

interface Props {
  currentUser: { isLoggedIn: boolean; login?: string };
  identityProviders: IdentityProvider[];
  onUpdateUsers: () => void;
  updateTokensCount: (login: string, tokensCount: number) => void;
  users: User[];
}

export default function UsersList({
  currentUser,
  identityProviders,
  onUpdateUsers,
  updateTokensCount,
  users,
}: Props) {
  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra" id="users-list">
        <thead>
          <tr>
            <th />
            <th className="nowrap" />
            <th className="nowrap">{translate('my_profile.scm_accounts')}</th>
            <th className="nowrap">{translate('users.last_connection')}</th>
            <th className="nowrap">{translate('my_profile.groups')}</th>
            <th className="nowrap">{translate('users.tokens')}</th>
            <th className="nowrap">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <UserListItem
              identityProvider={identityProviders.find(
                (provider) => user.externalProvider === provider.key
              )}
              isCurrentUser={currentUser.isLoggedIn && currentUser.login === user.login}
              key={user.login}
              onUpdateUsers={onUpdateUsers}
              updateTokensCount={updateTokensCount}
              user={user}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
