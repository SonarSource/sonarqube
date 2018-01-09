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
import Avatar from '../../../../components/ui/Avatar';
import Checkbox from '../../../../components/controls/Checkbox';
import { PermissionUser } from '../../../../api/permissions';
import { translate } from '../../../../helpers/l10n';

interface Props {
  user: PermissionUser;
  permissions: string[];
  selectedPermission?: string;
  permissionsOrder: string[];
  onToggle: (user: PermissionUser, permission: string) => void;
}

export default class UserHolder extends React.PureComponent<Props> {
  handleCheck = (_checked: boolean, permission?: string) =>
    permission && this.props.onToggle(this.props.user, permission);

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

    const { user } = this.props;
    if (user.login === '<creator>') {
      return (
        <tr>
          <td className="nowrap">
            <div className="display-inline-block text-middle">
              <div>
                <strong>{user.name}</strong>
              </div>
              <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
                {translate('permission_templates.project_creators.explanation')}
              </div>
            </div>
          </td>
          {permissionCells}
        </tr>
      );
    }

    return (
      <tr>
        <td className="nowrap">
          <Avatar
            hash={user.avatar}
            name={user.name}
            size={36}
            className="text-middle big-spacer-right"
          />
          <div className="display-inline-block text-middle">
            <div>
              <strong>{user.name}</strong>
              <span className="note spacer-left">{user.login}</span>
            </div>
            <div className="little-spacer-top">{user.email}</div>
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }
}
