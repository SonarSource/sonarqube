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
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { IdentityProvider } from '../../../types/types';
import { User } from '../../../types/users';
import TokensFormModal from './TokensFormModal';
import UserActions from './UserActions';
import UserGroups from './UserGroups';
import UserListItemIdentity from './UserListItemIdentity';
import UserScmAccounts from './UserScmAccounts';

interface Props {
  identityProvider?: IdentityProvider;
  isCurrentUser: boolean;
  onUpdateUsers: () => void;
  updateTokensCount: (login: string, tokensCount: number) => void;
  user: User;
}

interface State {
  openTokenForm: boolean;
}

export default class UserListItem extends React.PureComponent<Props, State> {
  state: State = { openTokenForm: false };

  handleOpenTokensForm = () => this.setState({ openTokenForm: true });
  handleCloseTokensForm = () => this.setState({ openTokenForm: false });

  render() {
    const { identityProvider, onUpdateUsers, user } = this.props;

    return (
      <tr>
        <td className="thin nowrap text-middle">
          <Avatar hash={user.avatar} name={user.name} size={36} />
        </td>
        <UserListItemIdentity identityProvider={identityProvider} user={user} />
        <td className="thin nowrap text-middle">
          <UserScmAccounts scmAccounts={user.scmAccounts || []} />
        </td>
        <td className="thin nowrap text-middle">
          <DateFromNow date={user.lastConnectionDate} hourPrecision={true} />
        </td>
        <td className="thin nowrap text-middle">
          <UserGroups groups={user.groups || []} onUpdateUsers={onUpdateUsers} user={user} />
        </td>
        <td className="thin nowrap text-middle">
          {user.tokensCount}
          <ButtonIcon
            className="js-user-tokens spacer-left button-small"
            onClick={this.handleOpenTokensForm}
            tooltip={translate('users.update_tokens')}
          >
            <BulletListIcon />
          </ButtonIcon>
        </td>
        <td className="thin nowrap text-right text-middle">
          <UserActions
            isCurrentUser={this.props.isCurrentUser}
            onUpdateUsers={onUpdateUsers}
            user={user}
          />
        </td>
        {this.state.openTokenForm && (
          <TokensFormModal
            onClose={this.handleCloseTokensForm}
            updateTokensCount={this.props.updateTokensCount}
            user={user}
          />
        )}
      </tr>
    );
  }
}
