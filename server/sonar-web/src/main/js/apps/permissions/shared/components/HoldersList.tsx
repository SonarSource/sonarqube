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
import UserHolder from './UserHolder';
import GroupHolder from './GroupHolder';
import PermissionHeader, { Permission } from './PermissionHeader';
import { PermissionUser, PermissionGroup } from '../../../../api/permissions';
import { translate } from '../../../../helpers/l10n';

interface Props {
  permissions: Permission[];
  users: PermissionUser[];
  groups: PermissionGroup[];
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
  onSelectPermission: (permission: string) => void;
  onToggleUser: (user: PermissionUser, permission: string) => void;
  onToggleGroup: (group: PermissionGroup, permission: string) => void;
}

export default class HoldersList extends React.PureComponent<Props> {
  renderTableHeader() {
    const { onSelectPermission, selectedPermission, showPublicProjectsWarning } = this.props;
    const cells = this.props.permissions.map(p => (
      <PermissionHeader
        key={p.key}
        onSelectPermission={onSelectPermission}
        permission={p}
        selectedPermission={selectedPermission}
        showPublicProjectsWarning={showPublicProjectsWarning}
      />
    ));
    return (
      <thead>
        <tr>
          <td className="nowrap bordered-bottom">{this.props.children}</td>
          {cells}
        </tr>
      </thead>
    );
  }

  renderEmpty() {
    const columns = this.props.permissions.length + 1;
    return (
      <tr>
        <td colSpan={columns}>{translate('no_results_search')}</td>
      </tr>
    );
  }

  render() {
    const permissionsOrder = this.props.permissions.map(p => p.key);

    const users = this.props.users.map(user => (
      <UserHolder
        key={'user-' + user.login}
        user={user}
        permissions={user.permissions}
        selectedPermission={this.props.selectedPermission}
        permissionsOrder={permissionsOrder}
        onToggle={this.props.onToggleUser}
      />
    ));

    const groups = this.props.groups.map(group => (
      <GroupHolder
        key={'group-' + group.id}
        group={group}
        permissions={group.permissions}
        selectedPermission={this.props.selectedPermission}
        permissionsOrder={permissionsOrder}
        onToggle={this.props.onToggleGroup}
      />
    ));

    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra permissions-table">
          {this.renderTableHeader()}
          <tbody>
            {users.length === 0 && groups.length === 0 && this.renderEmpty()}
            {users}
            {groups}
          </tbody>
        </table>
      </div>
    );
  }
}
