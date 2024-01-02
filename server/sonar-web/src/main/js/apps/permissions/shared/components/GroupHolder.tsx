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
import { without } from 'lodash';
import * as React from 'react';
import GroupIcon from '../../../../components/icons/GroupIcon';
import { translate } from '../../../../helpers/l10n';
import { Permissions } from '../../../../types/permissions';
import { PermissionDefinitions, PermissionGroup } from '../../../../types/types';
import { isPermissionDefinitionGroup } from '../../utils';
import PermissionCell from './PermissionCell';

interface Props {
  group: PermissionGroup;
  isComponentPrivate?: boolean;
  onToggle: (group: PermissionGroup, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  selectedPermission?: string;
}

interface State {
  loading: string[];
}

export const ANYONE = 'Anyone';

export default class GroupHolder extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: [] };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = (permission: string) => {
    if (this.mounted) {
      this.setState((state) => ({ loading: without(state.loading, permission) }));
    }
  };

  handleCheck = (_checked: boolean, permission?: string) => {
    if (permission !== undefined) {
      this.setState((state) => ({ loading: [...state.loading, permission] }));
      this.props.onToggle(this.props.group, permission).then(
        () => this.stopLoading(permission),
        () => this.stopLoading(permission)
      );
    }
  };

  render() {
    const { group, isComponentPrivate, permissions, selectedPermission } = this.props;

    return (
      <tr>
        <td className="nowrap text-middle">
          <div className="display-flex-center">
            <GroupIcon className="big-spacer-right" />
            <div className="max-width-100">
              <div className="max-width-100 text-ellipsis">
                <strong>{group.name}</strong>
                {group.name === ANYONE && (
                  <span className="spacer-left badge badge-error">{translate('deprecated')}</span>
                )}
              </div>
              <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
                {group.name === ANYONE
                  ? translate('user_groups.anyone.description')
                  : group.description}
              </div>
            </div>
          </div>
        </td>
        {permissions.map((permission) => {
          const isPermissionGroup = isPermissionDefinitionGroup(permission);
          const permissionKey = isPermissionGroup ? permission.category : permission.key;
          const isAdminPermission = !isPermissionGroup && permissionKey === Permissions.Admin;

          return (
            <PermissionCell
              disabled={group.name === ANYONE && (isComponentPrivate || isAdminPermission)}
              isGroupItem={true}
              key={permissionKey}
              loading={this.state.loading}
              onCheck={this.handleCheck}
              permission={permission}
              permissionItem={group}
              selectedPermission={selectedPermission}
            />
          );
        })}
      </tr>
    );
  }
}
