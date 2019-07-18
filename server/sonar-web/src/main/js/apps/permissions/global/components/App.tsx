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
import { without } from 'lodash';
import * as React from 'react';
import Helmet from 'react-helmet';
import { translate } from 'sonar-ui-common/helpers/l10n';
import * as api from '../../../../api/permissions';
import Suggestions from '../../../../app/components/embed-docs-modal/Suggestions';
import '../../styles.css';
import AllHoldersList from './AllHoldersList';
import PageHeader from './PageHeader';

interface Props {
  organization?: T.Organization;
}

interface State {
  filter: 'all' | 'groups' | 'users';
  groups: T.PermissionGroup[];
  groupsPaging?: T.Paging;
  loading: boolean;
  query: string;
  users: T.PermissionUser[];
  usersPaging?: T.Paging;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      filter: 'all',
      groups: [],
      loading: true,
      query: '',
      users: []
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
    const { organization } = this.props;
    const { filter, query } = this.state;

    const getUsers: Promise<{ paging?: T.Paging; users: T.PermissionUser[] }> =
      filter !== 'groups'
        ? api.getGlobalPermissionsUsers({
            q: query || undefined,
            organization: organization && organization.key,
            p: userPage
          })
        : Promise.resolve({ paging: undefined, users: [] });

    const getGroups: Promise<{ paging?: T.Paging; groups: T.PermissionGroup[] }> =
      filter !== 'users'
        ? api.getGlobalPermissionsGroups({
            q: query || undefined,
            organization: organization && organization.key,
            p: groupsPage
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
          usersPaging: usersResponse.paging
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
          usersPaging: usersResponse.paging
        }));
      }
    }, this.stopLoading);
  };

  onFilter = (filter: 'all' | 'groups' | 'users') => {
    this.setState({ filter }, this.loadHolders);
  };

  onSearch = (query: string) => {
    this.setState({ query }, this.loadHolders);
  };

  addPermissionToGroup = (groups: T.PermissionGroup[], group: string, permission: string) => {
    return groups.map(candidate =>
      candidate.name === group
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  addPermissionToUser = (users: T.PermissionUser[], user: string, permission: string) => {
    return users.map(candidate =>
      candidate.login === user
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate
    );
  };

  removePermissionFromGroup = (groups: T.PermissionGroup[], group: string, permission: string) => {
    return groups.map(candidate =>
      candidate.name === group
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  removePermissionFromUser = (users: T.PermissionUser[], user: string, permission: string) => {
    return users.map(candidate =>
      candidate.login === user
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate
    );
  };

  grantPermissionToGroup = (group: string, permission: string) => {
    if (this.mounted) {
      this.setState(({ groups }) => ({
        groups: this.addPermissionToGroup(groups, group, permission)
      }));
      return api
        .grantPermissionToGroup({
          groupName: group,
          permission,
          organization: this.props.organization && this.props.organization.key
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ groups }) => ({
                groups: this.removePermissionFromGroup(groups, group, permission)
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
        users: this.addPermissionToUser(users, user, permission)
      }));
      return api
        .grantPermissionToUser({
          login: user,
          permission,
          organization: this.props.organization && this.props.organization.key
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ users }) => ({
                users: this.removePermissionFromUser(users, user, permission)
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
        groups: this.removePermissionFromGroup(groups, group, permission)
      }));
      return api
        .revokePermissionFromGroup({
          groupName: group,
          permission,
          organization: this.props.organization && this.props.organization.key
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ groups }) => ({
                groups: this.addPermissionToGroup(groups, group, permission)
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
        users: this.removePermissionFromUser(users, user, permission)
      }));
      return api
        .revokePermissionFromUser({
          login: user,
          permission,
          organization: this.props.organization && this.props.organization.key
        })
        .then(
          () => {},
          () => {
            if (this.mounted) {
              this.setState(({ users }) => ({
                users: this.addPermissionToUser(users, user, permission)
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
    return (
      <div className="page page-limited">
        <Suggestions suggestions="global_permissions" />
        <Helmet title={translate('global_permissions.permission')} />
        <PageHeader loading={this.state.loading} organization={this.props.organization} />
        <AllHoldersList
          filter={this.state.filter}
          grantPermissionToGroup={this.grantPermissionToGroup}
          grantPermissionToUser={this.grantPermissionToUser}
          groups={this.state.groups}
          groupsPaging={this.state.groupsPaging}
          loadHolders={this.loadHolders}
          loading={this.state.loading}
          onFilter={this.onFilter}
          onLoadMore={this.onLoadMore}
          onSearch={this.onSearch}
          query={this.state.query}
          revokePermissionFromGroup={this.revokePermissionFromGroup}
          revokePermissionFromUser={this.revokePermissionFromUser}
          users={this.state.users}
          usersPaging={this.state.usersPaging}
        />
      </div>
    );
  }
}
