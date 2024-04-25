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
import { LargeCenteredLayout, PageContentFontWrapper } from 'design-system';
import { without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import * as api from '../../../../api/permissions';
import withAppStateContext, {
  WithAppStateContextProps,
} from '../../../../app/components/app-state/withAppStateContext';
import Suggestions from '../../../../components/embed-docs-modal/Suggestions';
import AllHoldersList from '../../../../components/permissions/AllHoldersList';
import { FilterOption } from '../../../../components/permissions/SearchForm';
import { translate } from '../../../../helpers/l10n';
import {
  PERMISSIONS_ORDER_GLOBAL,
  convertToPermissionDefinitions,
  filterPermissions,
} from '../../../../helpers/permissions';
import { Paging, PermissionGroup, PermissionUser } from '../../../../types/types';
import '../../styles.css';
import PageHeader from './PageHeader';

type Props = WithAppStateContextProps;

interface State {
  filter: FilterOption;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  loading: boolean;
  query: string;
  users: PermissionUser[];
  usersPaging?: Paging;
}

class PermissionsGlobalApp extends React.PureComponent<Props, State> {
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

    this.loadUsersAndGroups().then(([usersResponse, groupsResponse]) => {
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

  handleLoadMore = () => {
    const { usersPaging, groupsPaging } = this.state;
    this.setState({ loading: true });

    this.loadUsersAndGroups(
      usersPaging ? usersPaging.pageIndex + 1 : 1,
      groupsPaging ? groupsPaging.pageIndex + 1 : 1,
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

  handleFilter = (filter: FilterOption) => {
    this.setState({ filter }, this.loadHolders);
  };

  handleSearch = (query: string) => {
    this.setState({ query }, this.loadHolders);
  };

  addPermissionToGroup = (groups: PermissionGroup[], group: string, permission: string) => {
    return groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate,
    );
  };

  addPermissionToUser = (users: PermissionUser[], user: string, permission: string) => {
    return users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: [...candidate.permissions, permission] }
        : candidate,
    );
  };

  removePermissionFromGroup = (groups: PermissionGroup[], group: string, permission: string) => {
    return groups.map((candidate) =>
      candidate.name === group
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate,
    );
  };

  removePermissionFromUser = (users: PermissionUser[], user: string, permission: string) => {
    return users.map((candidate) =>
      candidate.login === user
        ? { ...candidate, permissions: without(candidate.permissions, permission) }
        : candidate,
    );
  };

  handleGrantPermissionToGroup = (group: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .grantPermissionToGroup({
        groupName: group,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState(({ groups }) => ({
            loading: false,
            groups: this.addPermissionToGroup(groups, group, permission),
          }));
        }
      }, this.stopLoading);
  };

  handleGrantPermissionToUser = (user: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .grantPermissionToUser({
        login: user,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState(({ users }) => ({
            loading: false,
            users: this.addPermissionToUser(users, user, permission),
          }));
        }
      }, this.stopLoading);
  };

  handleRevokePermissionFromGroup = (group: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .revokePermissionFromGroup({
        groupName: group,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState(({ groups }) => ({
            loading: false,
            groups: this.removePermissionFromGroup(groups, group, permission),
          }));
        }
      }, this.stopLoading);
  };

  handleRevokePermissionFromUser = (user: string, permission: string) => {
    this.setState({ loading: true });
    return api
      .revokePermissionFromUser({
        login: user,
        permission,
      })
      .then(() => {
        if (this.mounted) {
          this.setState(({ users }) => ({
            loading: false,
            users: this.removePermissionFromUser(users, user, permission),
          }));
        }
      }, this.stopLoading);
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
      'global_permissions',
    );
    return (
      <LargeCenteredLayout id="project-permissions-page">
        <PageContentFontWrapper className="sw-my-8 sw-body-sm">
          <Suggestions suggestions="global_permissions" />
          <Helmet defer={false} title={translate('global_permissions.permission')} />
          <PageHeader />
          <AllHoldersList
            permissions={permissions}
            filter={filter}
            onGrantPermissionToGroup={this.handleGrantPermissionToGroup}
            onGrantPermissionToUser={this.handleGrantPermissionToUser}
            groups={groups}
            groupsPaging={groupsPaging}
            loading={loading}
            onFilter={this.handleFilter}
            onLoadMore={this.handleLoadMore}
            onQuery={this.handleSearch}
            query={query}
            onRevokePermissionFromGroup={this.handleRevokePermissionFromGroup}
            onRevokePermissionFromUser={this.handleRevokePermissionFromUser}
            users={users}
            usersPaging={usersPaging}
          />
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withAppStateContext(PermissionsGlobalApp);
