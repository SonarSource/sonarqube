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
import { Badge, ContentCell, TableRowInteractive } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { isPermissionDefinitionGroup } from '../../helpers/permissions';
import { getBaseUrl } from '../../helpers/system';
import { useIdentityProviderQuery } from '../../queries/identity-provider/common';
import { Permissions } from '../../types/permissions';
import { PermissionDefinitions, PermissionGroup, Provider } from '../../types/types';
import GroupIcon from '../icons/GroupIcon';
import PermissionCell from './PermissionCell';
import usePermissionChange from './usePermissionChange';

interface Props {
  group: PermissionGroup;
  isComponentPrivate?: boolean;
  onToggle: (group: PermissionGroup, permission: string) => Promise<void>;
  permissions: PermissionDefinitions;
  selectedPermission?: string;
  disabled?: boolean;
  removeOnly?: boolean;
  isGitHubProject?: boolean;
}

export const ANYONE = 'Anyone';

export default function GroupHolder(props: Props) {
  const {
    group,
    isComponentPrivate,
    permissions,
    selectedPermission,
    disabled,
    removeOnly,
    isGitHubProject,
  } = props;
  const { loading, handleCheck, modal } = usePermissionChange({
    holder: group,
    onToggle: props.onToggle,
    permissions,
    removeOnly,
  });
  const { data: identityProvider } = useIdentityProviderQuery();

  const description =
    group.name === ANYONE ? translate('user_groups.anyone.description') : group.description;

  return (
    <TableRowInteractive>
      <ContentCell>
        <div className="sw-flex sw-items-center">
          <GroupIcon className="sw-mr-4" />
          <div className="sw-max-w-abs-800">
            <div className="sw-flex sw-w-fit sw-max-w-full">
              <div className="sw-flex-1 sw-text-ellipsis sw-whitespace-nowrap sw-overflow-hidden  sw-min-w-0">
                <strong>{group.name}</strong>
              </div>
              {isGitHubProject &&
                identityProvider?.provider === Provider.Github &&
                group.managed && (
                  <img
                    alt="github"
                    className="sw-ml-2"
                    aria-label={translate('project_permission.github_managed')}
                    height={16}
                    src={`${getBaseUrl()}/images/alm/github.svg`}
                  />
                )}
              {group.name === ANYONE && (
                <Badge className="sw-ml-2" variant="deleted">
                  {translate('deprecated')}
                </Badge>
              )}
            </div>
            {description && <div className="sw-mt-2 sw-whitespace-normal">{description}</div>}
          </div>
        </div>
      </ContentCell>
      {permissions.map((permission) => {
        const isPermissionGroup = isPermissionDefinitionGroup(permission);
        const permissionKey = isPermissionGroup ? permission.category : permission.key;
        const isAdminPermission = !isPermissionGroup && permissionKey === Permissions.Admin;

        return (
          <PermissionCell
            disabled={
              disabled || (group.name === ANYONE && (isComponentPrivate || isAdminPermission))
            }
            removeOnly={removeOnly}
            key={permissionKey}
            loading={loading}
            onCheck={handleCheck}
            permission={permission}
            permissionItem={group}
            prefixID={group.name}
            selectedPermission={selectedPermission}
          />
        );
      })}
      {modal}
    </TableRowInteractive>
  );
}
