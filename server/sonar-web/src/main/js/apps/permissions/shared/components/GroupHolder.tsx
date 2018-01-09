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
import Checkbox from '../../../../components/controls/Checkbox';
import GroupIcon from '../../../../components/icons-components/GroupIcon';
import { PermissionGroup } from '../../../../api/permissions';

interface Props {
  group: PermissionGroup;
  permissions: string[];
  selectedPermission?: string;
  permissionsOrder: string[];
  onToggle: (group: PermissionGroup, permission: string) => void;
}

export default class GroupHolder extends React.PureComponent<Props> {
  handleCheck = (_checked: boolean, permission?: string) =>
    permission && this.props.onToggle(this.props.group, permission);

  render() {
    const { selectedPermission } = this.props;
    const permissionCells = this.props.permissionsOrder.map(permission => (
      <td
        key={permission}
        className="text-center text-middle"
        style={{ backgroundColor: permission === selectedPermission ? '#d9edf7' : 'transparent' }}>
        <Checkbox
          checked={this.props.permissions.includes(permission)}
          id={permission}
          onCheck={this.handleCheck}
        />
      </td>
    ));

    const { group } = this.props;

    return (
      <tr>
        <td className="nowrap">
          <div className="display-inline-block text-middle big-spacer-right">
            <GroupIcon />
          </div>
          <div className="display-inline-block text-middle">
            <div>
              <strong>{group.name}</strong>
            </div>
            <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
              {group.description}
            </div>
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }
}
