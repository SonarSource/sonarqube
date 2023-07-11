/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { translate } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import { getBaseUrl } from '../../helpers/system';
import { PermissionDefinitions, PermissionUser } from '../../types/types';
import LegacyAvatar from '../ui/LegacyAvatar';
import PermissionCell from './PermissionCell';
import usePermissionChange from './usePermissionChange';

interface Props {
  onToggle: (user: PermissionUser, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  selectedPermission?: string;
  user: PermissionUser;
  isGitHubProject?: boolean;
  disabled?: boolean;
  removeOnly?: boolean;
}

export default function UserHolder(props: Props) {
  const { user, disabled, removeOnly, permissions, isGitHubProject, selectedPermission } = props;
  const { loading, handleCheck, modal } = usePermissionChange({
    holder: user,
    onToggle: props.onToggle,
    permissions,
    removeOnly,
  });

  const permissionCells = permissions.map((permission) => (
    <PermissionCell
      key={isPermissionDefinitionGroup(permission) ? permission.category : permission.key}
      loading={loading}
      onCheck={handleCheck}
      permission={permission}
      disabled={disabled}
      removeOnly={removeOnly}
      permissionItem={user}
      selectedPermission={selectedPermission}
    />
  ));

  if (user.login === '<creator>') {
    return (
      <tr>
        <td className="nowrap text-middle">
          <div>
            <strong>{user.name}</strong>
          </div>
          <div className="little-spacer-top" style={{ whiteSpace: 'normal' }}>
            {translate('permission_templates.project_creators.explanation')}
          </div>
        </td>
        {permissionCells}
      </tr>
    );
  }

  return (
    <tr>
      <td className="nowrap text-middle">
        <div className="display-flex-center">
          <LegacyAvatar
            className="text-middle big-spacer-right flex-0"
            hash={user.avatar}
            name={user.name}
            size={36}
          />
          <div className="max-width-100">
            <div className="sw-flex sw-w-fit sw-max-w-full">
              <div className="sw-flex-1 text-ellipsis">
                <strong>{user.name}</strong>
                <span className="note spacer-left">{user.login}</span>
              </div>
              {isGitHubProject && user.managed && (
                <img
                  alt="github"
                  className="spacer-left spacer-right"
                  height={16}
                  aria-label={translate('project_permission.github_managed')}
                  src={`${getBaseUrl()}/images/alm/github.svg`}
                />
              )}
            </div>

            <div className="little-spacer-top max-width-100 text-ellipsis">{user.email}</div>
          </div>
        </div>
      </td>
      {permissionCells}
      {modal}
    </tr>
  );
}
