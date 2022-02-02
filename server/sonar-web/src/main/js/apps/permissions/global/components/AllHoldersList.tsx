/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import withAppStateContext from '../../../../app/components/app-state/withAppStateContext';
import ListFooter from '../../../../components/controls/ListFooter';
import { ComponentQualifier } from '../../../../types/component';
import { AppState, Paging, PermissionGroup, PermissionUser } from '../../../../types/types';
import HoldersList from '../../shared/components/HoldersList';
import SearchForm from '../../shared/components/SearchForm';
import {
  convertToPermissionDefinitions,
  filterPermissions,
  PERMISSIONS_ORDER_GLOBAL
} from '../../utils';

interface StateProps {
  appState: AppState;
}

interface OwnProps {
  filter: string;
  grantPermissionToGroup: (groupName: string, permission: string) => Promise<void>;
  grantPermissionToUser: (login: string, permission: string) => Promise<void>;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  loadHolders: () => void;
  loading?: boolean;
  onLoadMore: () => void;
  onFilter: (filter: string) => void;
  onSearch: (query: string) => void;
  query: string;
  revokePermissionFromGroup: (groupName: string, permission: string) => Promise<void>;
  revokePermissionFromUser: (login: string, permission: string) => Promise<void>;
  users: PermissionUser[];
  usersPaging?: Paging;
}

type Props = StateProps & OwnProps;

export class AllHoldersList extends React.PureComponent<Props> {
  handleToggleUser = (user: PermissionUser, permission: string) => {
    const hasPermission = user.permissions.includes(permission);
    if (hasPermission) {
      return this.props.revokePermissionFromUser(user.login, permission);
    }
    return this.props.grantPermissionToUser(user.login, permission);
  };

  handleToggleGroup = (group: PermissionGroup, permission: string) => {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      return this.props.revokePermissionFromGroup(group.name, permission);
    }
    return this.props.grantPermissionToGroup(group.name, permission);
  };

  render() {
    const {
      appState,
      filter,
      groups,
      groupsPaging,
      users,
      usersPaging,
      loading,
      query
    } = this.props;
    const l10nPrefix = 'global_permissions';

    const hasPortfoliosEnabled = appState.qualifiers.includes(ComponentQualifier.Portfolio);
    const hasApplicationsEnabled = appState.qualifiers.includes(ComponentQualifier.Application);
    const permissions = convertToPermissionDefinitions(
      filterPermissions(PERMISSIONS_ORDER_GLOBAL, hasApplicationsEnabled, hasPortfoliosEnabled),
      l10nPrefix
    );

    let count = 0;
    let total = 0;
    if (filter !== 'users') {
      count += groups.length;
      total += groupsPaging ? groupsPaging.total : groups.length;
    }
    if (filter !== 'groups') {
      count += users.length;
      total += usersPaging ? usersPaging.total : users.length;
    }

    return (
      <>
        <HoldersList
          filter={filter}
          groups={groups}
          loading={loading}
          onToggleGroup={this.handleToggleGroup}
          onToggleUser={this.handleToggleUser}
          permissions={permissions}
          query={query}
          users={users}>
          <SearchForm
            filter={filter}
            onFilter={this.props.onFilter}
            onSearch={this.props.onSearch}
            query={query}
          />
        </HoldersList>
        <ListFooter count={count} loadMore={this.props.onLoadMore} total={total} />
      </>
    );
  }
}

export default withAppStateContext(AllHoldersList);
