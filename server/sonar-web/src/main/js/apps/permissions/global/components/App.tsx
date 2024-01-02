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
import { without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import * as api from '../../../../api/permissions';
import withAppStateContext from '../../../../app/components/app-state/withAppStateContext';
import Suggestions from '../../../../components/embed-docs-modal/Suggestions';
import { translate } from '../../../../helpers/l10n';
import { AppState } from '../../../../types/appstate';
import { ComponentQualifier } from '../../../../types/component';
import { Paging, PermissionGroup, PermissionUser } from '../../../../types/types';
import AllHoldersList from '../../shared/components/AllHoldersList';
import { FilterOption } from '../../shared/components/SearchForm';
import '../../styles.css';
import {
  convertToPermissionDefinitions,
  filterPermissions,
  PERMISSIONS_ORDER_GLOBAL,
} from '../../utils';
import PageHeader from './PageHeader';

interface Props {
  appState: AppState;
}
interface State {
  filter: FilterOption;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  loading: boolean;
  query: string;
  users: PermissionUser[];
  usersPaging?: Paging;
}
export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      filter: 'all',
      groups: [],
      loading: true,
      query: '',
      users: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadHolders();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadUsersAndGroups = (userPage?: number, groupsPage?: number) => {
    const { filter, query } = this.state;

    const getUsers: Promise<{ paging?: Paging; users: PermissionUser[] }> =
      filter !== 'groups'
        ? api.getGlobalPermissionsUsers({
            q: query || undefined,
            p: userPage,
          })
        : Promise.resolve({ paging: undefined, users: [] });

    const getGroups: Promise<{ paging?: Paging; groups: PermissionGroup[] }> =
      filter !== 'users'
        ? api.getGlobalPermissionsGroups({
            q: query || undefined,
            p: groupsPage,
          })
        : Promise.resolve({ paging: undefined, groups: [] });

    return Promise.all([getUsers, getGroups]);
  };

  loadHolders = () => {
    this.setState({ loading: true });
    return this.loadUsersAndGroups().then(([usersResponse, groupsResponse]) => {
      if (this.mounted) {
        this.setState({
          groups: groupsResponse.groups,
          groupsPaging: groupsResponse.paging,
          loading: false,
          users: usersResponse.users,
          usersPaging: usersResponse.paging,
        });
      }
    }, this.stopLoading);
  };

  onLoadMore = () => {
    const { usersPaging, groupsPaging } = this.state;
    this.setState({ loading: true });
    return this.loadUsersAndGroups(
      usersPaging ? usersPaging.pageIndex + 1 : 1,
      groupsPaging ? groupsPaging.pageIndex + 1 : 1
    ).then(([usersResponse, groupsResponse]) => {
      if (this.mounted) {
        this.setState(({ groups, users }) => ({
          groups: [...groups, ...groupsResponse.groups],
          groupsPaging: groupsResponse.paging,
          loading: false,
          users: [...users, ...usersResponse.users],
          usersPaging: usersResponse.paging,
        }));
      }
    }, this.stopLoading);
  };

  onFilter = (filter: FilterOption) => {
    this.setState({ filter }, this.loadHolders);
  };

  onSearch = (query: string) => {
    this.setState({ query }, this.loadHolders);
  };

  addPermissionToGroup = (groups: PermissionGroup[], group: string, permission: string) => {
    return groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  addPermissionToUser = (users: PermissionUser[], user: string, permission: string) => {
    return users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  removePermissionFromGroup = (groups: PermissionGroup[], group: string, permission: string) => {
    return groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  removePermissionFromUser = (users: PermissionUser[], user: string, permission: string) => {
    return users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  grantPermissionToGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState(({ groups }) => ({
        groups: this.addPermissionToGroup(groups, group, permission),
      }));
      return api
        .grantPermissionToGroup({
          groupName: group,
          permission,
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ groups }) => ({
                groups: this.removePermissionFromGroup(groups, group, permission),
              }));
            }
          }
        );
    }
    return Promise.resolve();
  };

  grantPermissionToUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState(({ users }) => ({
        users: this.addPermissionToUser(users, user, permission),
      }));
      return api
        .grantPermissionToUser({
          login: user,
          permission,
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ users }) => ({
                users: this.removePermissionFromUser(users, user, permission),
              }));
            }
          }
        );
    }
    return Promise.resolve();
  };

  revokePermissionFromGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState(({ groups }) => ({
        groups: this.removePermissionFromGroup(groups, group, permission),
      }));
      return api
        .revokePermissionFromGroup({
          groupName: group,
          permission,
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ groups }) => ({
                groups: this.addPermissionToGroup(groups, group, permission),
              }));
            }
          }
        );
    }
    return Promise.resolve();
  };

  revokePermissionFromUser = (user: string, permission: string) => {
    if (this.mounted) {
      this.setState(({ users }) => ({
        users: this.removePermissionFromUser(users, user, permission),
      }));
      return api
        .revokePermissionFromUser({
          login: user,
          permission,
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ users }) => ({
                users: this.addPermissionToUser(users, user, permission),
              }));
            }
          }
        );
    }
    return Promise.resolve();
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  render() {
    const { appState } = this.props;
    const { filter, groups, groupsPaging, users, usersPaging, loading, query } = this.state;

    const hasPortfoliosEnabled = appState.qualifiers.includes(ComponentQualifier.Portfolio);
    const hasApplicationsEnabled = appState.qualifiers.includes(ComponentQualifier.Application);
    const permissions = convertToPermissionDefinitions(
      filterPermissions(PERMISSIONS_ORDER_GLOBAL, hasApplicationsEnabled, hasPortfoliosEnabled),
      'global_permissions'
    );
    return (
      <div className="page page-limited">
        <Suggestions suggestions="global_permissions" />
        <Helmet defer={false} title={translate('global_permissions.permission')} />
        <PageHeader loading={loading} />
        <AllHoldersList
          permissions={permissions}
          filter={filter}
          grantPermissionToGroup={this.grantPermissionToGroup}
          grantPermissionToUser={this.grantPermissionToUser}
          groups={groups}
          groupsPaging={groupsPaging}
          loading={loading}
          onFilter={this.onFilter}
          onLoadMore={this.onLoadMore}
          onQuery={this.onSearch}
          query={query}
          revokePermissionFromGroup={this.revokePermissionFromGroup}
          revokePermissionFromUser={this.revokePermissionFromUser}
          users={users}
          usersPaging={usersPaging}
        />
      </div>
    );
  }
}

export default withAppStateContext(App);
