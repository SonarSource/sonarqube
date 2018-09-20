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
import { groupBy } from 'lodash';
import UserHolder from './UserHolder';
import GroupHolder from './GroupHolder';
import PermissionHeader from './PermissionHeader';
import { translate } from '../../../../helpers/l10n';
import { Permission, PermissionGroup, PermissionUser } from '../../../../app/types';

interface Props {
  loading?: boolean;
  groups: PermissionGroup[];
  onSelectPermission: (permission: string) => void;
  onToggleGroup: (group: PermissionGroup, permission: string) => Promise<void>;
  onToggleUser: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: Permission[];
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
  users: PermissionUser[];
}

export default class HoldersList extends React.PureComponent<Props> {
  isPermissionUser(item: PermissionGroup | PermissionUser): item is PermissionUser {
    return (item as PermissionUser).login !== undefined;
  }

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

  renderItem(item: PermissionUser | PermissionGroup, permissionsOrder: string[]) {
    return this.isPermissionUser(item)
      ? this.renderUser(item, permissionsOrder)
      : this.renderGroup(item, permissionsOrder);
  }

  renderUser(user: PermissionUser, permissionsOrder: string[]) {
    return (
      <UserHolder
        key={'user-' + user.login}
        onToggle={this.props.onToggleUser}
        permissions={user.permissions}
        permissionsOrder={permissionsOrder}
        selectedPermission={this.props.selectedPermission}
        user={user}
      />
    );
  }

  renderGroup(group: PermissionGroup, permissionsOrder: string[]) {
    return (
      <GroupHolder
        group={group}
        key={'group-' + group.id}
        onToggle={this.props.onToggleGroup}
        permissions={group.permissions}
        permissionsOrder={permissionsOrder}
        selectedPermission={this.props.selectedPermission}
      />
    );
  }

  render() {
    const permissionsOrder = this.props.permissions.map(p => p.key);
    const items = [...this.props.users, ...this.props.groups].sort((a, b) => {
      return a.name < b.name ? -1 : 1;
    });

    const { true: itemWithPermissions = [], false: itemWithoutPermissions = [] } = groupBy(
      items,
      item => item.permissions.length > 0
    );
    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra permissions-table">
          {this.renderTableHeader()}
          <tbody>
            {items.length === 0 && !this.props.loading && this.renderEmpty()}
            {itemWithPermissions.map(item => this.renderItem(item, permissionsOrder))}
            {itemWithPermissions.length > 0 &&
              itemWithoutPermissions.length > 0 && (
                <>
                  <tr>
                    <td className="divider" colSpan={6} />
                  </tr>
                  <tr /> {/* Keep correct zebra colors in the table */}
                </>
              )}
            {itemWithoutPermissions.map(item => this.renderItem(item, permissionsOrder))}
          </tbody>
        </table>
      </div>
    );
  }
}
