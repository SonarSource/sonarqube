/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Group } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import BulletListIcon from '../../../components/icons-components/BulletListIcon';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { ButtonIcon, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import {
  getUsersInGroup,
  addUserToGroup,
  removeUserFromGroup,
  GroupUser
} from '../../../api/user_groups';

interface Props {
  group: Group;
  onEdit: () => void;
  organization: string | undefined;
}

interface State {
  modal: boolean;
  users: GroupUser[];
  selectedUsers: string[];
}

export default class EditMembers extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = { modal: false, users: [], selectedUsers: [] };

  componentDidMount() {
    this.handleSearch('', Filter.Selected);
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (query: string, selected: Filter) => {
    return getUsersInGroup({
      id: this.props.group.id,
      organization: this.props.organization,
      ps: 100,
      q: query !== '' ? query : undefined,
      selected
    }).then(data => {
      this.setState({
        users: data.users,
        selectedUsers: data.users.filter(user => user.selected).map(user => user.login)
      });
    });
  };

  handleSelect = (login: string) => {
    return addUserToGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      this.setState((state: State) => ({
        selectedUsers: [...state.selectedUsers, login]
      }));
    });
  };

  handleUnselect = (login: string) => {
    return removeUserFromGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      this.setState((state: State) => ({
        selectedUsers: without(state.selectedUsers, login)
      }));
    });
  };

  handleMembersClick = () => {
    this.setState({ modal: true });
  };

  handleModalClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
      this.props.onEdit();
    }
  };

  renderElement = (login: string): React.ReactNode => {
    const user = find(this.state.users, { login });
    return user === undefined ? login : user.login;
  };

  render() {
    const modalHeader = translate('users.update');

    return (
      <>
        <ButtonIcon className="button-small" onClick={this.handleMembersClick}>
          <BulletListIcon />
        </ButtonIcon>
        {this.state.modal && (
          <Modal contentLabel={modalHeader} onRequestClose={this.handleModalClose}>
            <header className="modal-head">
              <h2>{modalHeader}</h2>
            </header>

            <div className="modal-body">
              <SelectList
                elements={this.state.users.map(user => user.login)}
                onSearch={this.handleSearch}
                onSelect={this.handleSelect}
                onUnselect={this.handleUnselect}
                renderElement={this.renderElement}
                selectedElements={this.state.selectedUsers}
              />
            </div>

            <footer className="modal-foot">
              <ResetButtonLink onClick={this.handleModalClose}>{translate('Done')}</ResetButtonLink>
            </footer>
          </Modal>
        )}
      </>
    );
  }
}
