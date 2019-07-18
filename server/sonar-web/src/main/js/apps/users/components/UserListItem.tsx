/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import BulletListIcon from 'sonar-ui-common/components/icons/BulletListIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DateFromNowHourPrecision from '../../../components/intl/DateFromNowHourPrecision';
import Avatar from '../../../components/ui/Avatar';
import TokensFormModal from './TokensFormModal';
import UserActions from './UserActions';
import UserGroups from './UserGroups';
import UserListItemIdentity from './UserListItemIdentity';
import UserScmAccounts from './UserScmAccounts';

interface Props {
  identityProvider?: T.IdentityProvider;
  isCurrentUser: boolean;
  onUpdateUsers: () => void;
  organizationsEnabled?: boolean;
  updateTokensCount: (login: string, tokensCount: number) => void;
  user: T.User;
}

interface State {
  openTokenForm: boolean;
}

export default class UserListItem extends React.PureComponent<Props, State> {
  state: State = { openTokenForm: false };

  handleOpenTokensForm = () => this.setState({ openTokenForm: true });
  handleCloseTokensForm = () => this.setState({ openTokenForm: false });

  render() {
    const { identityProvider, onUpdateUsers, organizationsEnabled, user } = this.props;

    return (
      <tr>
        <td className="thin nowrap">
          <Avatar hash={user.avatar} name={user.name} size={36} />
        </td>
        <UserListItemIdentity identityProvider={identityProvider} user={user} />
        <td>
          <UserScmAccounts scmAccounts={user.scmAccounts || []} />
        </td>
        <td>
          <DateFromNowHourPrecision date={user.lastConnectionDate} />
        </td>
        {!organizationsEnabled && (
          <td>
            <UserGroups groups={user.groups || []} onUpdateUsers={onUpdateUsers} user={user} />
          </td>
        )}
        <td>
          {user.tokensCount}
          <ButtonIcon
            className="js-user-tokens spacer-left button-small"
            onClick={this.handleOpenTokensForm}
            tooltip={translate('users.update_tokens')}>
            <BulletListIcon />
          </ButtonIcon>
        </td>
        <td className="thin nowrap text-right">
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
