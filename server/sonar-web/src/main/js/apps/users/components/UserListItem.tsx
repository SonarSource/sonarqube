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
import { IconMoreVertical, Spinner, Tooltip } from '@sonarsource/echoes-react';
import { ActionCell, Avatar, ContentCell, InteractiveIcon, TableRow } from 'design-system';
import * as React from 'react';
import DateFromNow from '../../../components/intl/DateFromNow';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useUserGroupsCountQuery } from '../../../queries/group-memberships';
import { useUserTokensQuery } from '../../../queries/users';
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

export default function UserListItem(props: Readonly<UserListItemProps>) {
  const { identityProvider, user, manageProvider } = props;
  const {
    id,
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
  const { data: groupsCount, isLoading: groupsAreLoading } = useUserGroupsCountQuery(id);

  return (
    <TableRow>
      <ContentCell>
        <div className="sw-flex sw-items-center">
          <Avatar className="sw-shrink-0 sw-mr-4" hash={avatar} name={name} size="md" />
          <UserListItemIdentity
            identityProvider={identityProvider}
            user={user}
            manageProvider={manageProvider}
          />
        </div>
      </ContentCell>
      <ContentCell>
        <UserScmAccounts scmAccounts={scmAccounts || []} />
      </ContentCell>
      <ContentCell>
        <DateFromNow date={sonarQubeLastConnectionDate ?? ''} hourPrecision />
      </ContentCell>
      <ContentCell>
        <DateFromNow date={sonarLintLastConnectionDate ?? ''} hourPrecision />
      </ContentCell>
      <ContentCell>
        <Spinner isLoading={groupsAreLoading}>
          {groupsCount}
          {manageProvider === undefined && (
            <Tooltip content={translate('users.update_groups')}>
              <InteractiveIcon
                Icon={IconMoreVertical}
                className="it__user-groups sw-ml-2"
                aria-label={translateWithParameters('users.update_users_groups', user.login)}
                onClick={() => setOpenGroupForm(true)}
                size="small"
              />
            </Tooltip>
          )}
        </Spinner>
      </ContentCell>
      <ContentCell>
        <Spinner isLoading={tokensAreLoading}>
          {tokens?.length}
          <Tooltip content={translateWithParameters('users.update_tokens')}>
            <InteractiveIcon
              Icon={IconMoreVertical}
              className="it__user-tokens sw-ml-2"
              aria-label={translateWithParameters('users.update_tokens_for_x', name ?? login)}
              onClick={() => setOpenTokenForm(true)}
              size="small"
            />
          </Tooltip>
        </Spinner>
      </ContentCell>

      <ActionCell>
        <UserActions user={user} manageProvider={manageProvider} />
      </ActionCell>

      {openTokenForm && <TokensFormModal onClose={() => setOpenTokenForm(false)} user={user} />}
      {openGroupForm && <GroupsForm onClose={() => setOpenGroupForm(false)} user={user} />}
    </TableRow>
  );
}
