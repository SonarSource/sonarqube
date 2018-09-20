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
import { without } from 'lodash';
import SearchForm from '../../shared/components/SearchForm';
import HoldersList from '../../shared/components/HoldersList';
import { translate } from '../../../../helpers/l10n';
import { PERMISSIONS_ORDER_BY_QUALIFIER } from '../constants';
import {
  Component,
  Paging,
  PermissionGroup,
  PermissionUser,
  Visibility
} from '../../../../app/types';
import ListFooter from '../../../../components/controls/ListFooter';

interface Props {
  component: Component;
  filter: string;
  grantPermissionToGroup: (group: string, permission: string) => Promise<void>;
  grantPermissionToUser: (user: string, permission: string) => Promise<void>;
  groups: PermissionGroup[];
  groupsPaging: Paging;
  onLoadMore: (usersPageIndex: number, groupsPageIndex: number) => void;
  onFilterChange: (filter: string) => void;
  onPermissionSelect: (permissions?: string) => void;
  onQueryChange: (query: string) => void;
  query: string;
  revokePermissionFromGroup: (group: string, permission: string) => Promise<void>;
  revokePermissionFromUser: (user: string, permission: string) => Promise<void>;
  selectedPermission?: string;
  users: PermissionUser[];
  usersPaging: Paging;
  visibility?: Visibility;
}

export default class AllHoldersList extends React.PureComponent<Props> {
  handleToggleUser = (user: PermissionUser, permission: string) => {
    const hasPermission = user.permissions.includes(permission);

    if (hasPermission) {
      return this.props.revokePermissionFromUser(user.login, permission);
    } else {
      return this.props.grantPermissionToUser(user.login, permission);
    }
  };

  handleToggleGroup = (group: PermissionGroup, permission: string) => {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      return this.props.revokePermissionFromGroup(group.name, permission);
    } else {
      return this.props.grantPermissionToGroup(group.name, permission);
    }
  };

  handleSelectPermission = (permission?: string) => {
    this.props.onPermissionSelect(permission);
  };

  handleLoadMore = () => {
    this.props.onLoadMore(
      this.props.usersPaging.pageIndex + 1,
      this.props.groupsPaging.pageIndex + 1
    );
  };

  render() {
    let order = PERMISSIONS_ORDER_BY_QUALIFIER[this.props.component.qualifier];
    if (this.props.visibility === Visibility.Public) {
      order = without(order, 'user', 'codeviewer');
    }

    const permissions = order.map(p => ({
      key: p,
      name: translate('projects_role', p),
      description: translate('projects_role', p, 'desc')
    }));

    const count =
      (this.props.filter !== 'users' ? this.props.groups.length : 0) +
      (this.props.filter !== 'groups' ? this.props.users.length : 0);
    const total =
      (this.props.filter !== 'users' ? this.props.groupsPaging.total : 0) +
      (this.props.filter !== 'groups' ? this.props.usersPaging.total : 0);

    return (
      <>
        <HoldersList
          groups={this.props.groups}
          onSelectPermission={this.handleSelectPermission}
          onToggleGroup={this.handleToggleGroup}
          onToggleUser={this.handleToggleUser}
          permissions={permissions}
          selectedPermission={this.props.selectedPermission}
          users={this.props.users}>
          <SearchForm
            filter={this.props.filter}
            onFilter={this.props.onFilterChange}
            onSearch={this.props.onQueryChange}
            query={this.props.query}
          />
        </HoldersList>
        <ListFooter count={count} loadMore={this.handleLoadMore} total={total} />
      </>
    );
  }
}
