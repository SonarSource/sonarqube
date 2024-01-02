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
import { ButtonIcon } from '../../../components/controls/buttons';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import DateFromNow from '../../../components/intl/DateFromNow';
import LegacyAvatar from '../../../components/ui/LegacyAvatar';
import Spinner from '../../../components/ui/Spinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useUserGroupsCountQuery, useUserTokensQuery } from '../../../queries/users';
import { IdentityProvider, Provider } from '../../../types/types';
import { RestUserDetailed } from '../../../types/users';
import GroupsForm from './GroupsForm';
import TokensFormModal from './TokensFormModal';
import UserActions from './UserActions';
import UserListItemIdentity from './UserListItemIdentity';
import UserScmAccounts from './UserScmAccounts';

export interface UserListItemProps {
  identityProvider?: IdentityProvider;
  user: RestUserDetailed;
  manageProvider: Provider | undefined;
}

export default function UserListItem(props: UserListItemProps) {
  const { identityProvider, user, manageProvider } = props;
  const {
    name,
    login,
    avatar,
    sonarQubeLastConnectionDate,
    sonarLintLastConnectionDate,
    scmAccounts,
  } = user;

  const [openTokenForm, setOpenTokenForm] = React.useState(false);
  const [openGroupForm, setOpenGroupForm] = React.useState(false);
  const { data: tokens, isLoading: tokensAreLoading } = useUserTokensQuery(login);
  const { data: groupsCount, isLoading: groupsAreLoading } = useUserGroupsCountQuery(login);

  return (
    <tr>
      <td className="thin text-middle">
        <div className="sw-flex sw-items-center">
          <LegacyAvatar className="sw-shrink-0 sw-mr-4" hash={avatar} name={name} size={36} />
          <UserListItemIdentity
            identityProvider={identityProvider}
            user={user}
            manageProvider={manageProvider}
          />
        </div>
      </td>
      <td className="thin text-middle">
        <UserScmAccounts scmAccounts={scmAccounts || []} />
      </td>
      <td className="thin nowrap text-middle">
        <DateFromNow date={sonarQubeLastConnectionDate ?? ''} hourPrecision />
      </td>
      <td className="thin nowrap text-middle">
        <DateFromNow date={sonarLintLastConnectionDate ?? ''} hourPrecision />
      </td>
      <td className="thin nowrap text-middle">
        <Spinner loading={groupsAreLoading}>
          {groupsCount}
          {manageProvider === undefined && (
            <ButtonIcon
              aria-label={translateWithParameters('users.update_users_groups', user.login)}
              className="js-user-groups spacer-left button-small"
              onClick={() => setOpenGroupForm(true)}
              tooltip={translate('users.update_groups')}
            >
              <BulletListIcon />
            </ButtonIcon>
          )}
        </Spinner>
      </td>
      <td className="thin nowrap text-middle">
        <Spinner loading={tokensAreLoading}>
          {tokens?.length}
          <ButtonIcon
            className="js-user-tokens spacer-left button-small"
            onClick={() => setOpenTokenForm(true)}
            tooltip={translateWithParameters('users.update_tokens')}
            aria-label={translateWithParameters('users.update_tokens_for_x', name ?? login)}
          >
            <BulletListIcon />
          </ButtonIcon>
        </Spinner>
      </td>

      <td className="thin nowrap text-right text-middle">
        <UserActions user={user} manageProvider={manageProvider} />
      </td>

      {openTokenForm && <TokensFormModal onClose={() => setOpenTokenForm(false)} user={user} />}
      {openGroupForm && <GroupsForm onClose={() => setOpenGroupForm(false)} user={user} />}
    </tr>
  );
}
