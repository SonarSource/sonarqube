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
import { Permissions } from '../types/permissions';
import { Dict, PermissionDefinition, PermissionDefinitionGroup } from '../types/types';
import { translate } from './l10n';

export const PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE = [
  Permissions.Browse,
  Permissions.CodeViewer,
  Permissions.IssueAdmin,
  Permissions.SecurityHotspotAdmin,
  Permissions.Admin,
  Permissions.Scan,
];

export const PERMISSIONS_ORDER_GLOBAL = [
  Permissions.Admin,
  {
    category: 'administer',
    permissions: [Permissions.QualityGateAdmin, Permissions.QualityProfileAdmin],
  },
  Permissions.Scan,
  {
    category: 'creator',
    permissions: [
      Permissions.ProjectCreation,
      Permissions.ApplicationCreation,
      Permissions.PortfolioCreation,
    ],
  },
];

export const PERMISSIONS_ORDER_FOR_VIEW = [Permissions.Browse, Permissions.Admin];

export const PERMISSIONS_ORDER_BY_QUALIFIER: Dict<string[]> = {
  TRK: PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  VW: PERMISSIONS_ORDER_FOR_VIEW,
  SVW: PERMISSIONS_ORDER_FOR_VIEW,
  APP: PERMISSIONS_ORDER_FOR_VIEW,
};

function convertToPermissionDefinition(permission: string, l10nPrefix: string) {
  const name = translate(`${l10nPrefix}.${permission}`);
  const description = translate(`${l10nPrefix}.${permission}.desc`);

  return {
    key: permission,
    name,
    description,
  };
}

export function filterPermissions(
  permissions: Array<Permissions | { category: string; permissions: Permissions[] }>,
  hasApplicationsEnabled: boolean,
  hasPortfoliosEnabled: boolean,
) {
  return permissions.map((permission) => {
    if (typeof permission === 'object' && permission.category === 'creator') {
      return {
        ...permission,
        permissions: permission.permissions.filter((p) => {
          return (
            p === Permissions.ProjectCreation ||
            (p === Permissions.PortfolioCreation && hasPortfoliosEnabled) ||
            (p === Permissions.ApplicationCreation && hasApplicationsEnabled)
          );
        }),
      };
    }
    return permission;
  });
}

export function convertToPermissionDefinitions(
  permissions: Array<string | { category: string; permissions: string[] }>,
  l10nPrefix: string,
): Array<PermissionDefinition | PermissionDefinitionGroup> {
  return permissions.map((permission) => {
    if (typeof permission === 'object') {
      return {
        category: permission.category,
        permissions: permission.permissions.map((permission) =>
          convertToPermissionDefinition(permission, l10nPrefix),
        ),
      };
    }
    return convertToPermissionDefinition(permission, l10nPrefix);
  });
}

export function isPermissionDefinitionGroup(
  permission?: PermissionDefinition | PermissionDefinitionGroup,
): permission is PermissionDefinitionGroup {
  return Boolean(permission && (permission as PermissionDefinitionGroup).category);
}
