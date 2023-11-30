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
import { Avatar, ContentCell, Note, TableRowInteractive } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import { getBaseUrl } from '../../helpers/system';
import { PermissionDefinitions, PermissionUser } from '../../types/types';
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
      prefixID={user.login}
      selectedPermission={selectedPermission}
    />
  ));

  if (user.login === '<creator>') {
    return (
      <TableRowInteractive>
        <ContentCell>
          <div className="sw-max-w-abs-800">
            <div className="sw-flex sw-flex-col sw-w-fit sw-max-w-full">
              <strong className="sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                {user.name}
              </strong>
              <p className="sw-mt-2">
                {translate('permission_templates.project_creators.explanation')}
              </p>
            </div>
          </div>
        </ContentCell>
        {permissionCells}
      </TableRowInteractive>
    );
  }

  return (
    <TableRowInteractive>
      <ContentCell>
        <div className="sw-flex sw-items-center">
          <Avatar className="sw-mr-4" hash={user.avatar} name={user.name} size="md" />
          <div className="sw-max-w-abs-800">
            <div className="sw-flex sw-w-fit sw-max-w-full">
              <div className="sw-flex-1 sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                <strong>{user.name}</strong>
                <Note className="sw-ml-2">{user.login}</Note>
              </div>
              {isGitHubProject && user.managed && (
                <img
                  alt="github"
                  className="sw-my-2"
                  height={16}
                  aria-label={translate('project_permission.github_managed')}
                  src={`${getBaseUrl()}/images/alm/github.svg`}
                />
              )}
            </div>
            {user.email && (
              <div className="sw-mt-2 sw-max-w-100 sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden">
                {user.email}
              </div>
            )}
          </div>
        </div>
      </ContentCell>
      {permissionCells}
      {modal}
    </TableRowInteractive>
  );
}
