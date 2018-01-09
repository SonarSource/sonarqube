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
import SearchForm from '../../shared/components/SearchForm';
import HoldersList from '../../shared/components/HoldersList';
import { translate } from '../../../../helpers/l10n';
import { Organization } from '../../../../app/types';
import { PermissionUser, PermissionGroup } from '../../../../api/permissions';

const PERMISSIONS_ORDER = ['admin', 'profileadmin', 'gateadmin', 'scan', 'provisioning'];

interface Props {
  filter: string;
  grantPermissionToGroup: (groupName: string, permission: string) => void;
  grantPermissionToUser: (login: string, permission: string) => void;
  groups: PermissionGroup[];
  loadHolders: () => void;
  onFilter: (filter: string) => void;
  onSearch: (query: string) => void;
  onSelectPermission: (permission: string) => void;
  organization?: Organization;
  query: string;
  revokePermissionFromGroup: (groupName: string, permission: string) => void;
  revokePermissionFromUser: (login: string, permission: string) => void;
  selectedPermission?: string;
  users: PermissionUser[];
}

export default class AllHoldersList extends React.PureComponent<Props> {
  componentDidMount() {
    this.props.loadHolders();
  }

  handleToggleUser = (user: PermissionUser, permission: string) => {
    const hasPermission = user.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromUser(user.login, permission);
    } else {
      this.props.grantPermissionToUser(user.login, permission);
    }
  };

  handleToggleGroup = (group: PermissionGroup, permission: string) => {
    const hasPermission = group.permissions.includes(permission);

    if (hasPermission) {
      this.props.revokePermissionFromGroup(group.name, permission);
    } else {
      this.props.grantPermissionToGroup(group.name, permission);
    }
  };

  render() {
    const l10nPrefix = this.props.organization ? 'organizations_permissions' : 'global_permissions';
    const permissions = PERMISSIONS_ORDER.map(p => ({
      key: p,
      name: translate(l10nPrefix, p),
      description: translate(l10nPrefix, p, 'desc')
    }));

    return (
      <HoldersList
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        users={this.props.users}
        groups={this.props.groups}
        onSelectPermission={this.props.onSelectPermission}
        onToggleUser={this.handleToggleUser}
        onToggleGroup={this.handleToggleGroup}>
        <SearchForm
          query={this.props.query}
          filter={this.props.filter}
          onSearch={this.props.onSearch}
          onFilter={this.props.onFilter}
        />
      </HoldersList>
    );
  }
}
