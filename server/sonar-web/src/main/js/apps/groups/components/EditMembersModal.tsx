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
import {
  addUserToGroup,
  getUsersInGroup,
  GroupUser,
  removeUserFromGroup
} from '../../../api/user_groups';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  group: T.Group;
  onClose: () => void;
  organization: string | undefined;
}

export interface SearchParams {
  name: string;
  organization?: string;
  page: number;
  pageSize: number;
  query?: string;
  selected: string;
}

interface State {
  lastSearchParams: SearchParams;
  listHasBeenTouched: boolean;
  loading: boolean;
  users: GroupUser[];
  usersTotalCount?: number;
  selectedUsers: string[];
}

const PAGE_SIZE = 100;

export default class EditMembersModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      lastSearchParams: {
        name: props.group.name,
        organization: props.organization,
        page: 1,
        pageSize: PAGE_SIZE,
        query: '',
        selected: Filter.Selected
      },
      listHasBeenTouched: false,
      loading: true,
      users: [],
      selectedUsers: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchUsers(this.state.lastSearchParams);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchUsers = (searchParams: SearchParams, more?: boolean) =>
    getUsersInGroup({
      ...searchParams,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined
    }).then(
      data => {
        if (this.mounted) {
          this.setState(prevState => {
            const users = more ? [...prevState.users, ...data.users] : data.users;
            const newSelectedUsers = data.users
              .filter(user => user.selected)
              .map(user => user.login);
            const selectedUsers = more
              ? [...prevState.selectedUsers, ...newSelectedUsers]
              : newSelectedUsers;

            return {
              lastSearchParams: searchParams,
              listHasBeenTouched: false,
              loading: false,
              users,
              usersTotalCount: data.total,
              selectedUsers
            };
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );

  handleLoadMore = () =>
    this.fetchUsers(
      {
        ...this.state.lastSearchParams,
        page: this.state.lastSearchParams.page + 1
      },
      true
    );

  handleReload = () =>
    this.fetchUsers({
      ...this.state.lastSearchParams,
      page: 1
    });

  handleSearch = (query: string, selected: Filter) =>
    this.fetchUsers({
      ...this.state.lastSearchParams,
      page: 1,
      query,
      selected
    });

  handleSelect = (login: string) =>
    addUserToGroup({
      name: this.props.group.name,
      login,
      organization: this.props.organization
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          listHasBeenTouched: true,
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
          listHasBeenTouched: true,
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
          <DeferredSpinner loading={this.state.loading}>
            <SelectList
              elements={this.state.users.map(user => user.login)}
              elementsTotalCount={this.state.usersTotalCount}
              needReload={
                this.state.listHasBeenTouched && this.state.lastSearchParams.selected !== Filter.All
              }
              onLoadMore={this.handleLoadMore}
              onReload={this.handleReload}
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
