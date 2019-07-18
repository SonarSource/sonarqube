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
import * as React from 'react';
import { connect } from 'react-redux';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import { getAppState, Store } from '../../../../store/rootReducer';
import HoldersList from '../../shared/components/HoldersList';
import SearchForm from '../../shared/components/SearchForm';
import {
  convertToPermissionDefinitions,
  PERMISSIONS_ORDER_GLOBAL,
  PERMISSIONS_ORDER_GLOBAL_GOV
} from '../../utils';

interface StateProps {
  appState: Pick<T.AppState, 'qualifiers'>;
}

interface OwnProps {
  filter: string;
  grantPermissionToGroup: (groupName: string, permission: string) => Promise<void>;
  grantPermissionToUser: (login: string, permission: string) => Promise<void>;
  groups: T.PermissionGroup[];
  groupsPaging?: T.Paging;
  loadHolders: () => void;
  loading?: boolean;
  onLoadMore: () => void;
  onFilter: (filter: string) => void;
  onSearch: (query: string) => void;
  organization?: T.Organization;
  query: string;
  revokePermissionFromGroup: (groupName: string, permission: string) => Promise<void>;
  revokePermissionFromUser: (login: string, permission: string) => Promise<void>;
  users: T.PermissionUser[];
  usersPaging?: T.Paging;
}

type Props = StateProps & OwnProps;

export class AllHoldersList extends React.PureComponent<Props> {
  handleToggleUser = (user: T.PermissionUser, permission: string) => {
    const hasPermission = user.permissions.includes(permission);
    if (hasPermission) {
      return this.props.revokePermissionFromUser(user.login, permission);
    } else {
      return this.props.grantPermissionToUser(user.login, permission);
    }
  };

  handleToggleGroup = (group: T.PermissionGroup, permission: string) => {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      return this.props.revokePermissionFromGroup(group.name, permission);
    } else {
      return this.props.grantPermissionToGroup(group.name, permission);
    }
  };

  render() {
    const { filter, groups, groupsPaging, users, usersPaging } = this.props;
    const l10nPrefix = this.props.organization ? 'organizations_permissions' : 'global_permissions';
    const governanceInstalled = this.props.appState.qualifiers.includes('VW');
    const permissions = convertToPermissionDefinitions(
      governanceInstalled ? PERMISSIONS_ORDER_GLOBAL_GOV : PERMISSIONS_ORDER_GLOBAL,
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
          filter={this.props.filter}
          groups={this.props.groups}
          loading={this.props.loading}
          onToggleGroup={this.handleToggleGroup}
          onToggleUser={this.handleToggleUser}
          permissions={permissions}
          query={this.props.query}
          users={this.props.users}>
          <SearchForm
            filter={this.props.filter}
            onFilter={this.props.onFilter}
            onSearch={this.props.onSearch}
            query={this.props.query}
          />
        </HoldersList>
        <ListFooter count={count} loadMore={this.props.onLoadMore} total={total} />
      </>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  appState: getAppState(state)
});

export default connect(mapStateToProps)(AllHoldersList);
