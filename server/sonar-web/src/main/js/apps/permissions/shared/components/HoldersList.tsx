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
import { partition, sortBy } from 'lodash';
import UserHolder from './UserHolder';
import GroupHolder from './GroupHolder';
import PermissionHeader from './PermissionHeader';
import { translate } from '../../../../helpers/l10n';
import { PermissionGroup, PermissionUser, PermissionDefinitions } from '../../../../app/types';
import { isPermissionDefinitionGroup } from '../../utils';

interface Props {
  loading?: boolean;
  groups: PermissionGroup[];
  onSelectPermission?: (permission: string) => void;
  onToggleGroup: (group: PermissionGroup, permission: string) => Promise<void>;
  onToggleUser: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
  users: PermissionUser[];
}

export default class HoldersList extends React.PureComponent<Props> {
  isPermissionUser(item: PermissionGroup | PermissionUser): item is PermissionUser {
    return (item as PermissionUser).login !== undefined;
  }

  renderEmpty() {
    const columns = this.props.permissions.length + 1;
    return (
      <tr>
        <td colSpan={columns}>{translate('no_results_search')}</td>
      </tr>
    );
  }

  renderItem(item: PermissionUser | PermissionGroup, permissions: PermissionDefinitions) {
    return this.isPermissionUser(item) ? (
      <UserHolder
        key={`user-${item.login}`}
        onToggle={this.props.onToggleUser}
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        user={item}
      />
    ) : (
      <GroupHolder
        group={item}
        key={`group-${item.id || item.name}`}
        onToggle={this.props.onToggleGroup}
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
      />
    );
  }

  render() {
    const { permissions } = this.props;
    const items = sortBy([...this.props.users, ...this.props.groups], item => {
      if (this.isPermissionUser(item) && item.login === '<creator>') {
        return 0;
      }
      return item.name;
    });
    const [itemWithPermissions, itemWithoutPermissions] = partition(
      items,
      item => item.permissions.length > 0
    );
    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra permissions-table">
          <thead>
            <tr>
              <td className="nowrap bordered-bottom">{this.props.children}</td>
              {permissions.map(permission => (
                <PermissionHeader
                  key={
                    isPermissionDefinitionGroup(permission) ? permission.category : permission.key
                  }
                  onSelectPermission={this.props.onSelectPermission}
                  permission={permission}
                  selectedPermission={this.props.selectedPermission}
                  showPublicProjectsWarning={this.props.showPublicProjectsWarning}
                />
              ))}
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && !this.props.loading && this.renderEmpty()}
            {itemWithPermissions.map(item => this.renderItem(item, permissions))}
            {itemWithPermissions.length > 0 &&
              itemWithoutPermissions.length > 0 && (
                <>
                  <tr>
                    <td className="divider" colSpan={20} />
                  </tr>
                  <tr />
                  {/* Keep correct zebra colors in the table */}
                </>
              )}
            {itemWithoutPermissions.map(item => this.renderItem(item, permissions))}
          </tbody>
        </table>
      </div>
    );
  }
}
