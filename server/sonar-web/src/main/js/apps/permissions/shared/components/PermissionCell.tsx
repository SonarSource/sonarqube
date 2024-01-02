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
import classNames from 'classnames';
import * as React from 'react';
import Checkbox from '../../../../components/controls/Checkbox';
import {
  PermissionDefinition,
  PermissionDefinitionGroup,
  PermissionGroup,
  PermissionUser,
} from '../../../../types/types';
import { isPermissionDefinitionGroup } from '../../utils';

interface Props {
  disabled?: boolean;
  isGroupItem?: boolean;
  loading: string[];
  onCheck: (checked: boolean, permission?: string) => void;
  permission: PermissionDefinition | PermissionDefinitionGroup;
  permissionItem: PermissionGroup | PermissionUser;
  selectedPermission?: string;
}

export default class PermissionCell extends React.PureComponent<Props> {
  render() {
    const {
      disabled,
      isGroupItem,
      loading,
      onCheck,
      permission,
      permissionItem,
      selectedPermission,
    } = this.props;

    const tenant = `${isGroupItem ? 'group' : 'user'} '${permissionItem.name}'`;

    if (isPermissionDefinitionGroup(permission)) {
      return (
        <td className="text-middle">
          {permission.permissions.map((permissionDefinition) => {
            const isChecked = permissionItem.permissions.includes(permissionDefinition.key);
            const isDisabled = disabled || loading.includes(permissionDefinition.key);
            let state = isChecked ? 'checked' : 'unchecked';

            if (isDisabled) {
              state = 'disabled';
            }

            return (
              <div key={permissionDefinition.key}>
                <Checkbox
                  checked={isChecked}
                  disabled={isDisabled}
                  id={permissionDefinition.key}
                  label={`${state} permission '${permissionDefinition.name}' for ${tenant}`}
                  onCheck={onCheck}
                >
                  <span className="little-spacer-left">{permissionDefinition.name}</span>
                </Checkbox>
              </div>
            );
          })}
        </td>
      );
    }

    const isChecked = permissionItem.permissions.includes(permission.key);
    const isDisabled = disabled || loading.includes(permission.key);
    let state = isChecked ? 'checked' : 'unchecked';

    if (isDisabled) {
      state = 'disabled';
    }

    return (
      <td
        className={classNames('permission-column text-center text-middle', {
          selected: permission.key === selectedPermission,
        })}
      >
        <Checkbox
          checked={isChecked}
          disabled={isDisabled}
          id={permission.key}
          label={`${state} permission '${permission.name}' for ${tenant}`}
          onCheck={onCheck}
        />
      </td>
    );
  }
}
