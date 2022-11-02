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
import { cloneDeep } from 'lodash';
import { PermissionTemplate } from '../../types/types';
import { BaseSearchProjectsParameters } from '../components';
import { bulkApplyTemplate, getPermissionTemplates } from '../permissions';

const MAX_PROJECTS_TO_APPLY_PERMISSION_TEMPLATE = 10;

const defaultPermissionTemplates: PermissionTemplate[] = [
  {
    id: 'template1',
    name: 'Permission Template 1',
    createdAt: '',
    defaultFor: [],
    permissions: [],
  },
  {
    id: 'template2',
    name: 'Permission Template 2',
    createdAt: '',
    defaultFor: [],
    permissions: [],
  },
];

export default class PermissionTemplateServiceMock {
  permissionTemplates: PermissionTemplate[] = [];

  constructor() {
    this.permissionTemplates = cloneDeep(defaultPermissionTemplates);
    (getPermissionTemplates as jest.Mock).mockImplementation(this.handleGetPermissionTemplates);
    (bulkApplyTemplate as jest.Mock).mockImplementation(this.handleBulkApplyTemplate);
  }

  handleGetPermissionTemplates = () => {
    return Promise.resolve({ permissionTemplates: cloneDeep(this.permissionTemplates) });
  };

  handleBulkApplyTemplate = (params: BaseSearchProjectsParameters) => {
    if (
      params.projects &&
      params.projects.split(',').length > MAX_PROJECTS_TO_APPLY_PERMISSION_TEMPLATE
    ) {
      const response = new Response(
        JSON.stringify({ errors: [{ msg: 'bulk apply permission template error message' }] })
      );
      return Promise.reject(response);
    }

    return Promise.resolve();
  };

  reset = () => {
    this.permissionTemplates = cloneDeep(defaultPermissionTemplates);
  };
}
