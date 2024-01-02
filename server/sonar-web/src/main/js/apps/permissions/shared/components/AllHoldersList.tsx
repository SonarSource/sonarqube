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
import * as React from 'react';
import ListFooter from '../../../../components/controls/ListFooter';
import {
  Paging,
  PermissionDefinition,
  PermissionDefinitionGroup,
  PermissionGroup,
  PermissionUser,
} from '../../../../types/types';
import HoldersList from '../../shared/components/HoldersList';
import SearchForm, { FilterOption } from '../../shared/components/SearchForm';

interface Props {
  filter: FilterOption;
  query: string;
  onFilter: (filter: string) => void;
  onQuery: (query: string) => void;
  groups: PermissionGroup[];
  groupsPaging?: Paging;
  revokePermissionFromGroup: (group: string, permission: string) => Promise<void>;
  grantPermissionToGroup: (group: string, permission: string) => Promise<void>;
  users: PermissionUser[];
  usersPaging?: Paging;
  revokePermissionFromUser: (user: string, permission: string) => Promise<void>;
  grantPermissionToUser: (user: string, permission: string) => Promise<void>;
  permissions: Array<PermissionDefinition | PermissionDefinitionGroup>;
  onLoadMore: () => void;
  selectedPermission?: string;
  onSelectPermission?: (permissions?: string) => void;
  loading?: boolean;
}

export default class AllHoldersList extends React.PureComponent<Props> {
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

  getPaging = () => {
    const { filter, groups, groupsPaging, users, usersPaging } = this.props;

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

    return { count, total };
  };

  render() {
    const { filter, query, groups, users, permissions, selectedPermission, loading } = this.props;
    const { count, total } = this.getPaging();

    return (
      <>
        <HoldersList
          loading={loading}
          filter={filter}
          groups={groups}
          onSelectPermission={this.props.onSelectPermission}
          onToggleGroup={this.handleToggleGroup}
          onToggleUser={this.handleToggleUser}
          permissions={permissions}
          query={query}
          selectedPermission={selectedPermission}
          users={users}
        >
          <SearchForm
            filter={filter}
            onFilter={this.props.onFilter}
            onSearch={this.props.onQuery}
            query={query}
          />
        </HoldersList>
        <ListFooter count={count} loadMore={this.props.onLoadMore} total={total} />
      </>
    );
  }
}
