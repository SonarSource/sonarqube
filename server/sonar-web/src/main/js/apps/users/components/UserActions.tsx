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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isUserActive } from '../../../helpers/users';
import DeactivateForm from './DeactivateForm';
import PasswordForm from './PasswordForm';
import UserForm from './UserForm';

interface Props {
  isCurrentUser: boolean;
  onUpdateUsers: () => void;
  user: T.User;
}

interface State {
  openForm?: string;
}

export default class UserActions extends React.PureComponent<Props, State> {
  state: State = {};

  handleOpenDeactivateForm = () => {
    this.setState({ openForm: 'deactivate' });
  };

  handleOpenPasswordForm = () => {
    this.setState({ openForm: 'password' });
  };

  handleOpenUpdateForm = () => {
    this.setState({ openForm: 'update' });
  };

  handleCloseForm = () => {
    this.setState({ openForm: undefined });
  };

  renderActions = () => {
    const { user } = this.props;
    return (
      <ActionsDropdown>
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
        {isUserActive(user) && (
          <ActionsDropdownItem
            className="js-user-deactivate"
            destructive={true}
            onClick={this.handleOpenDeactivateForm}>
            {translate('users.deactivate')}
          </ActionsDropdownItem>
        )}
      </ActionsDropdown>
    );
  };

  render() {
    const { openForm } = this.state;
    const { isCurrentUser, onUpdateUsers, user } = this.props;

    return (
      <>
        {this.renderActions()}
        {openForm === 'deactivate' && isUserActive(user) && (
          <DeactivateForm
            onClose={this.handleCloseForm}
            onUpdateUsers={onUpdateUsers}
            user={user}
          />
        )}
        {openForm === 'password' && (
          <PasswordForm isCurrentUser={isCurrentUser} onClose={this.handleCloseForm} user={user} />
        )}
        {openForm === 'update' && (
          <UserForm onClose={this.handleCloseForm} onUpdateUsers={onUpdateUsers} user={user} />
        )}
      </>
    );
  }
}
