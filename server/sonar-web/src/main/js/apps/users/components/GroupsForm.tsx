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
import { getUserGroups, UserGroup } from '../../../api/users';
import { addUserToGroup, removeUserFromGroup } from '../../../api/user_groups';
import Modal from '../../../components/controls/Modal';
import SelectList, { Filter } from '../../../components/SelectList/SelectList';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onUpdateUsers: () => void;
  user: T.User;
}

export interface SearchParams {
  login: string;
  organization?: string;
  page: number;
  pageSize: number;
  query?: string;
  selected: string;
}

interface State {
  groups: UserGroup[];
  groupsTotalCount?: number;
  lastSearchParams: SearchParams;
  listHasBeenTouched: boolean;
  selectedGroups: string[];
}

const PAGE_SIZE = 100;

export default class GroupsForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      groups: [],
      lastSearchParams: {
        login: props.user.login,
        page: 1,
        pageSize: PAGE_SIZE,
        query: '',
        selected: Filter.Selected
      },
      listHasBeenTouched: false,
      selectedGroups: []
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
    getUserGroups({
      login: searchParams.login,
      organization: searchParams.organization !== '' ? searchParams.organization : undefined,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.selected
    }).then(data => {
      if (this.mounted) {
        this.setState(prevState => {
          const groups = more ? [...prevState.groups, ...data.groups] : data.groups;
          const newSeletedGroups = data.groups.filter(gp => gp.selected).map(gp => gp.name);
          const selectedGroups = more
            ? [...prevState.selectedGroups, ...newSeletedGroups]
            : newSeletedGroups;

          return {
            lastSearchParams: searchParams,
            listHasBeenTouched: false,
            groups,
            groupsTotalCount: data.paging.total,
            selectedGroups
          };
        });
      }
    });

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

  handleSelect = (name: string) =>
    addUserToGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          listHasBeenTouched: true,
          selectedGroups: [...state.selectedGroups, name]
        }));
      }
    });

  handleUnselect = (name: string) =>
    removeUserFromGroup({
      name,
      login: this.props.user.login
    }).then(() => {
      if (this.mounted) {
        this.setState((state: State) => ({
          listHasBeenTouched: true,
          selectedGroups: without(state.selectedGroups, name)
        }));
      }
    });

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.handleClose();
  };

  handleClose = () => {
    this.props.onUpdateUsers();
    this.props.onClose();
  };

  renderElement = (name: string): React.ReactNode => {
    const group = find(this.state.groups, { name });
    return (
      <div className="select-list-list-item">
        {group === undefined ? (
          name
        ) : (
          <>
            {group.name}
            <br />
            <span className="note">{group.description}</span>
          </>
        )}
      </div>
    );
  };

  render() {
    const header = translate('users.update_groups');

    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body modal-container">
          <SelectList
            elements={this.state.groups.map(group => group.name)}
            elementsTotalCount={this.state.groupsTotalCount}
            needReload={
              this.state.listHasBeenTouched && this.state.lastSearchParams.selected !== Filter.All
            }
            onLoadMore={this.handleLoadMore}
            onReload={this.handleReload}
            onSearch={this.handleSearch}
            onSelect={this.handleSelect}
            onUnselect={this.handleUnselect}
            renderElement={this.renderElement}
            selectedElements={this.state.selectedGroups}
          />
        </div>

        <footer className="modal-foot">
          <a className="js-modal-close" href="#" onClick={this.handleCloseClick}>
            {translate('Done')}
          </a>
        </footer>
      </Modal>
    );
  }
}
