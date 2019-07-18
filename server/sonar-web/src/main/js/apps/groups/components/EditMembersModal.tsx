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
import { find, without } from 'lodash';
import * as React from 'react';
import { ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams
} from 'sonar-ui-common/components/controls/SelectList';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addUserToGroup, getUsersInGroup, removeUserFromGroup } from '../../../api/user_groups';

interface Props {
  group: T.Group;
  onClose: () => void;
  organization: string | undefined;
}

interface State {
  lastSearchParams?: SelectListSearchParams;
  needToReload: boolean;
  users: T.UserSelected[];
  usersTotalCount?: number;
  selectedUsers: string[];
}

export default class EditMembersModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      needToReload: false,
      users: [],
      selectedUsers: []
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchUsers = (searchParams: SelectListSearchParams) =>
    getUsersInGroup({
      name: this.props.group.name,
      organization: this.props.organization,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const more = searchParams.page != null && searchParams.page > 1;

          const users = more ? [...prevState.users, ...data.users] : data.users;
          const newSelectedUsers = data.users.filter(user => user.selected).map(user => user.login);
          const selectedUsers = more
            ? [...prevState.selectedUsers, ...newSelectedUsers]
            : newSelectedUsers;

          return {
            needToReload: false,
            lastSearchParams: searchParams,
            loading: false,
            users,
            usersTotalCount: data.total,
            selectedUsers
          };
        });
      }
    });

  handleSelect = (login: string) =>
    addUserToGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedUsers: [...state.selectedUsers, login]
        }));
      }
    });

  handleUnselect = (login: string) =>
    removeUserFromGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          needToReload: true,
          selectedUsers: without(state.selectedUsers, login)
        }));
      }
    });

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

        <div className="modal-body modal-container">
          <SelectList
            elements={this.state.users.map(user => user.login)}
            elementsTotalCount={this.state.usersTotalCount}
            needToReload={
              this.state.needToReload &&
              this.state.lastSearchParams &&
              this.state.lastSearchParams.filter !== SelectListFilter.All
            }
            onSearch={this.fetchUsers}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedUsers}
            withPaging={true}
          />
        </div>

        <footer className="modal-foot">
          <ResetButtonLink onClick={this.props.onClose}>{translate('Done')}</ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
