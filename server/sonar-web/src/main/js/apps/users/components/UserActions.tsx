/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import DeactivateForm from './DeactivateForm';
import PasswordForm from './PasswordForm';
import UserForm from './UserForm';
import { User } from '../../../api/users';
import { translate } from '../../../helpers/l10n';

interface Props {
  isCurrentUser: boolean;
  onUpdateUsers: () => void;
  user: User;
}

interface State {
  openForm?: string;
}

export default class UserActions extends React.PureComponent<Props, State> {
  state: State = {};

  handleOpenDeactivateForm = () => this.setState({ openForm: 'deactivate' });
  handleOpenPasswordForm = () => this.setState({ openForm: 'password' });
  handleOpenUpdateForm = () => this.setState({ openForm: 'update' });
  handleCloseForm = () => this.setState({ openForm: undefined });

  renderActions = () => {
    const { user } = this.props;
    return (
      <ActionsDropdown key="actions" menuClassName="dropdown-menu-right">
        <ActionsDropdownItem className="js-user-update" onClick={this.handleOpenUpdateForm}>
          {translate('update_details')}
        </ActionsDropdownItem>
        {user.local && (
          <ActionsDropdownItem
            className="js-user-change-password"
            onClick={this.handleOpenPasswordForm}>
            {translate('my_profile.password.title')}
          </ActionsDropdownItem>
        )}
        <ActionsDropdownDivider />
        <ActionsDropdownItem
          className="js-user-deactivate"
          destructive={true}
          onClick={this.handleOpenDeactivateForm}>
          {translate('users.deactivate')}
        </ActionsDropdownItem>
      </ActionsDropdown>
    );
  };

  render() {
    const { openForm } = this.state;
    const { isCurrentUser, onUpdateUsers, user } = this.props;

    if (openForm === 'deactivate') {
      return [
        this.renderActions(),
        <DeactivateForm
          key="form"
          onClose={this.handleCloseForm}
          onUpdateUsers={onUpdateUsers}
          user={user}
        />
      ];
    }
    if (openForm === 'password') {
      return [
        this.renderActions(),
        <PasswordForm
          isCurrentUser={isCurrentUser}
          key="form"
          onClose={this.handleCloseForm}
          user={user}
        />
      ];
    }
    if (openForm === 'update') {
      return [
        this.renderActions(),
        <UserForm
          key="form"
          onClose={this.handleCloseForm}
          onUpdateUsers={onUpdateUsers}
          user={user}
        />
      ];
    }
    return this.renderActions();
  }
}
