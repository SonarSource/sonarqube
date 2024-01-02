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
import { partition } from 'lodash';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import {
  Dict,
  PermissionDefinitions,
  PermissionGroup,
  PermissionUser,
} from '../../../../types/types';
import { isPermissionDefinitionGroup } from '../../utils';
import GroupHolder from './GroupHolder';
import PermissionHeader from './PermissionHeader';
import UserHolder from './UserHolder';

interface Props {
  filter?: string;
  groups: PermissionGroup[];
  isComponentPrivate?: boolean;
  loading?: boolean;
  onSelectPermission?: (permission: string) => void;
  onToggleGroup: (group: PermissionGroup, permission: string) => Promise<void>;
  onToggleUser: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  query?: string;
  selectedPermission?: string;
  showPublicProjectsWarning?: boolean;
  users: PermissionUser[];
}

interface State {
  initialPermissionsCount: Dict<number>;
}
export default class HoldersList extends React.PureComponent<Props, State> {
  state: State = { initialPermissionsCount: {} };
  componentDidUpdate(prevProps: Props) {
    if (this.props.filter !== prevProps.filter || this.props.query !== prevProps.query) {
      this.setState({ initialPermissionsCount: {} });
    }
  }

  isPermissionUser(item: PermissionGroup | PermissionUser): item is PermissionUser {
    return (item as PermissionUser).login !== undefined;
  }

  handleGroupToggle = (group: PermissionGroup, permission: string) => {
    const key = group.id || group.name;
    if (this.state.initialPermissionsCount[key] === undefined) {
      this.setState((state) => ({
        initialPermissionsCount: {
          ...state.initialPermissionsCount,
          [key]: group.permissions.length,
        },
      }));
    }
    return this.props.onToggleGroup(group, permission);
  };

  handleUserToggle = (user: PermissionUser, permission: string) => {
    if (this.state.initialPermissionsCount[user.login] === undefined) {
      this.setState((state) => ({
        initialPermissionsCount: {
          ...state.initialPermissionsCount,
          [user.login]: user.permissions.length,
        },
      }));
    }
    return this.props.onToggleUser(user, permission);
  };

  getItemInitialPermissionsCount = (item: PermissionGroup | PermissionUser) => {
    const key = this.isPermissionUser(item) ? item.login : item.id || item.name;
    return this.state.initialPermissionsCount[key] !== undefined
      ? this.state.initialPermissionsCount[key]
      : item.permissions.length;
  };

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
        onToggle={this.handleUserToggle}
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
        user={item}
      />
    ) : (
      <GroupHolder
        group={item}
        isComponentPrivate={this.props.isComponentPrivate}
        key={`group-${item.id || item.name}`}
        onToggle={this.handleGroupToggle}
        permissions={permissions}
        selectedPermission={this.props.selectedPermission}
      />
    );
  }

  render() {
    const {
      permissions,
      users,
      groups,
      loading,
      children,
      selectedPermission,
      showPublicProjectsWarning,
    } = this.props;
    const items = [...groups, ...users];
    const [itemWithPermissions, itemWithoutPermissions] = partition(items, (item) =>
      this.getItemInitialPermissionsCount(item)
    );

    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra permissions-table">
          <thead>
            <tr>
              <td className="nowrap bordered-bottom">{children}</td>
              {permissions.map((permission) => (
                <PermissionHeader
                  key={
                    isPermissionDefinitionGroup(permission) ? permission.category : permission.key
                  }
                  onSelectPermission={this.props.onSelectPermission}
                  permission={permission}
                  selectedPermission={selectedPermission}
                  showPublicProjectsWarning={showPublicProjectsWarning}
                />
              ))}
            </tr>
          </thead>
          <tbody>
            {items.length === 0 && !loading && this.renderEmpty()}
            {itemWithPermissions.map((item) => this.renderItem(item, permissions))}
            {itemWithPermissions.length > 0 && itemWithoutPermissions.length > 0 && (
              <>
                <tr>
                  <td className="divider" colSpan={20} />
                </tr>
                <tr />
                {/* Keep correct zebra colors in the table */}
              </>
            )}
            {itemWithoutPermissions.map((item) => this.renderItem(item, permissions))}
          </tbody>
        </table>
      </div>
    );
  }
}
