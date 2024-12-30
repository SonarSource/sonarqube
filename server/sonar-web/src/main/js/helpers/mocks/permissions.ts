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

import {
  Permission,
  PermissionGroup,
  PermissionTemplate,
  PermissionTemplateGroup,
  PermissionUser,
} from '../../types/types';
import { mockUser } from '../testMocks';

export function mockPermissionGroup(overrides: Partial<PermissionGroup> = {}): PermissionGroup {
  return {
    name: 'sonar-admins',
    permissions: ['provisioning'],
    ...overrides,
  };
}

export function mockPermissionUser(overrides: Partial<PermissionUser> = {}): PermissionUser {
  return {
    ...mockUser(),
    active: true,
    name: 'johndoe',
    permissions: ['provisioning'],
    ...overrides,
  };
}

export function mockPermission(override: Partial<Permission> = {}) {
  return {
    key: 'admin',
    name: 'Admin',
    description: 'Can do anything he/she wants',
    ...override,
  };
}

export function mockPermissionTemplateGroup(override: Partial<PermissionTemplateGroup> = {}) {
  return {
    groupsCount: 1,
    usersCount: 1,
    key: 'admin',
    withProjectCreator: true,
    ...override,
  };
}

export function mockPermissionTemplate(override: Partial<PermissionTemplate> = {}) {
  return {
    id: 'template1',
    name: 'Permission Template 1',
    createdAt: '',
    defaultFor: [],
    permissions: [mockPermissionTemplateGroup()],
    ...override,
  };
}
