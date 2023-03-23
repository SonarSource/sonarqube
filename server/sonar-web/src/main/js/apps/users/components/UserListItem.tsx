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
import { ButtonIcon } from '../../../components/controls/buttons';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import Avatar from '../../../components/ui/Avatar';
import { translateWithParameters } from '../../../helpers/l10n';
import { IdentityProvider } from '../../../types/types';
import { User } from '../../../types/users';
import TokensFormModal from './TokensFormModal';
import UserActions from './UserActions';
import UserGroups from './UserGroups';
import UserListItemIdentity from './UserListItemIdentity';
import UserScmAccounts from './UserScmAccounts';

export interface UserListItemProps {
  identityProvider?: IdentityProvider;
  isCurrentUser: boolean;
  onUpdateUsers: () => void;
  updateTokensCount: (login: string, tokensCount: number) => void;
  user: User;
  manageProvider: string | undefined;
}

export default function UserListItem(props: UserListItemProps) {
  const [openTokenForm, setOpenTokenForm] = React.useState(false);

  const {
    identityProvider,
    onUpdateUsers,
    user,
    manageProvider,
    isCurrentUser,
    updateTokensCount,
  } = props;

  return (
    <tr>
      <td className="thin nowrap text-middle">
        <Avatar hash={user.avatar} name={user.name} size={36} />
      </td>
      <UserListItemIdentity
        identityProvider={identityProvider}
        user={user}
        manageProvider={manageProvider}
      />
      <td className="thin nowrap text-middle">
        <UserScmAccounts scmAccounts={user.scmAccounts || []} />
      </td>
      <td className="thin nowrap text-middle">
        <DateFromNow date={user.lastConnectionDate} hourPrecision={true} />
      </td>
      <td className="thin nowrap text-middle">
        <UserGroups
          groups={user.groups || []}
          manageProvider={manageProvider}
          onUpdateUsers={onUpdateUsers}
          user={user}
        />
      </td>
      <td className="thin nowrap text-middle">
        {user.tokensCount}
        <ButtonIcon
          className="js-user-tokens spacer-left button-small"
          onClick={() => setOpenTokenForm(true)}
          tooltip={translateWithParameters('users.update_tokens')}
          aria-label={translateWithParameters('users.update_tokens_for_x', user.name ?? user.login)}
        >
          <BulletListIcon />
        </ButtonIcon>
      </td>

      {(manageProvider === undefined || !user.managed) && (
        <td className="thin nowrap text-right text-middle">
          <UserActions
            isCurrentUser={isCurrentUser}
            onUpdateUsers={onUpdateUsers}
            user={user}
            manageProvider={manageProvider}
          />
        </td>
      )}

      {openTokenForm && (
        <TokensFormModal
          onClose={() => setOpenTokenForm(false)}
          updateTokensCount={updateTokensCount}
          user={user}
        />
      )}
    </tr>
  );
}
