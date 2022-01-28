/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import Checkbox from '../../../../components/controls/Checkbox';
import {
  PermissionDefinition,
  PermissionDefinitionGroup,
  PermissionGroup,
  PermissionUser
} from '../../../../types/types';
import { isPermissionDefinitionGroup } from '../../utils';

interface Props {
  loading: string[];
  onCheck: (checked: boolean, permission?: string) => void;
  permission: PermissionDefinition | PermissionDefinitionGroup;
  permissionItem: PermissionGroup | PermissionUser;
  selectedPermission?: string;
}

export default class PermissionCell extends React.PureComponent<Props> {
  render() {
    const { loading, onCheck, permission, permissionItem, selectedPermission } = this.props;
    if (isPermissionDefinitionGroup(permission)) {
      return (
        <td className="text-middle">
          {permission.permissions.map(permission => (
            <div key={permission.key}>
              <Checkbox
                checked={permissionItem.permissions.includes(permission.key)}
                disabled={loading.includes(permission.key)}
                id={permission.key}
                onCheck={onCheck}>
                <span className="little-spacer-left">{permission.name}</span>
              </Checkbox>
            </div>
          ))}
        </td>
      );
    } else {
      return (
        <td
          className={classNames('permission-column text-center text-middle', {
            selected: permission.key === selectedPermission
          })}>
          <Checkbox
            checked={permissionItem.permissions.includes(permission.key)}
            disabled={loading.includes(permission.key)}
            id={permission.key}
            onCheck={onCheck}
          />
        </td>
      );
    }
  }
}
