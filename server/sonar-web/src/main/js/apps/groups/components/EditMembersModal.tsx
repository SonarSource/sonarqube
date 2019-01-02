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
import { find, without } from 'lodash';
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import {
  GroupUser,
  removeUserFromGroup,
  addUserToGroup,
  getUsersInGroup
} from '../../../api/user_groups';
import DeferredSpinner from '../../../components/common/DeferredSpinner';

interface Props {
  group: T.Group;
  onClose: () => void;
  organization: string | undefined;
}

interface State {
  loading: boolean;
  users: GroupUser[];
  selectedUsers: string[];
}

export default class EditMembers extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, users: [], selectedUsers: [] };

  componentDidMount() {
    this.mounted = true;
    this.handleSearch('', Filter.Selected);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (query: string, selected: Filter) => {
    return getUsersInGroup({
      name: this.props.group.name,
      organization: this.props.organization,
      ps: 100,
      q: query !== '' ? query : undefined,
      selected
    }).then(
      data => {
        if (this.mounted) {
          this.setState({
            loading: false,
            users: data.users,
            selectedUsers: data.users.filter(user => user.selected).map(user => user.login)
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleSelect = (login: string) => {
    return addUserToGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          selectedUsers: [...state.selectedUsers, login]
        }));
      }
    });
  };

  handleUnselect = (login: string) => {
    return removeUserFromGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          selectedUsers: without(state.selectedUsers, login)
        }));
      }
    });
  };

  renderElement = (login: string): React.ReactNode => {
    const user = find(this.state.users, { login });
    return (
      <div className="select-list-list-item">
        {user === undefined ? (
          login
        ) : (
          <>
            {user.name}
            <br />
            <span className="note">{user.login}</span>
          </>
        )}
      </div>
    );
  };

  render() {
    const modalHeader = translate('users.update');
    return (
      <Modal contentLabel={modalHeader} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{modalHeader}</h2>
        </header>

        <div className="modal-body">
          <DeferredSpinner loading={this.state.loading}>
            <SelectList
              elements={this.state.users.map(user => user.login)}
              onSearch={this.handleSearch}
              onSelect={this.handleSelect}
              onUnselect={this.handleUnselect}
              renderElement={this.renderElement}
              selectedElements={this.state.selectedUsers}
            />
          </DeferredSpinner>
        </div>

        <footer className="modal-foot">
          <ResetButtonLink onClick={this.props.onClose}>{translate('Done')}</ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
