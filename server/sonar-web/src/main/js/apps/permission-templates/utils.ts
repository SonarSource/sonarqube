/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { sortBy } from 'lodash';

export const PERMISSIONS_ORDER = [
  'user',
  'codeviewer',
  'issueadmin',
  'securityhotspotadmin',
  'admin',
  'scan'
];

export function sortPermissions(permissions: T.Permission[]) {
  return sortBy(permissions, p => PERMISSIONS_ORDER.indexOf(p.key));
}

export function mergePermissionsToTemplates(
  permissionTemplates: T.PermissionTemplate[],
  basePermissions: T.Permission[]
): T.PermissionTemplate[] {
  return permissionTemplates.map(permissionTemplate => {
    // it's important to keep the order of the permission template's permissions
    // the same as the order of base permissions
    const permissions = basePermissions.map(basePermission => {
      const projectPermission = permissionTemplate.permissions.find(
        p => p.key === basePermission.key
      );
      return { usersCount: 0, groupsCount: 0, ...basePermission, ...projectPermission };
    });

    return { ...permissionTemplate, permissions };
  });
}

export function mergeDefaultsToTemplates(
  permissionTemplates: T.PermissionTemplate[],
  defaultTemplates: Array<{ templateId: string; qualifier: string }> = []
): T.PermissionTemplate[] {
  return permissionTemplates.map(permissionTemplate => {
    const defaultFor: string[] = [];

    defaultTemplates.forEach(defaultTemplate => {
      if (defaultTemplate.templateId === permissionTemplate.id) {
        defaultFor.push(defaultTemplate.qualifier);
      }
    });

    return { ...permissionTemplate, defaultFor };
  });
}
