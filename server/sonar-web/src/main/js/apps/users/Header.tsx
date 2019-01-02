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
import UserForm from './components/UserForm';
import DeferredSpinner from '../../components/common/DeferredSpinner';
import { Button } from '../../components/ui/buttons';
import { translate } from '../../helpers/l10n';

interface Props {
  loading: boolean;
  onUpdateUsers: () => void;
}

interface State {
  openUserForm: boolean;
}

export default class Header extends React.PureComponent<Props, State> {
  state: State = { openUserForm: false };

  handleOpenUserForm = () => {
    this.setState({ openUserForm: true });
  };

  handleCloseUserForm = () => {
    this.setState({ openUserForm: false });
  };

  render() {
    return (
      <header className="page-header" id="users-header">
        <h1 className="page-title">{translate('users.page')}</h1>
        <DeferredSpinner loading={this.props.loading} />

        <div className="page-actions">
          <Button id="users-create" onClick={this.handleOpenUserForm}>
            {translate('users.create_user')}
          </Button>
        </div>

        <p className="page-description">{translate('users.page.description')}</p>
        {this.state.openUserForm && (
          <UserForm onClose={this.handleCloseUserForm} onUpdateUsers={this.props.onUpdateUsers} />
        )}
      </header>
    );
  }
}
