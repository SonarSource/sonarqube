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
import { chunk, cloneDeep } from 'lodash';
import {
  mockPermissionTemplate,
  mockTemplateGroup,
  mockTemplateUser,
} from '../../helpers/testMocks';
import { PermissionTemplate } from '../../types/types';
import { BaseSearchProjectsParameters } from '../components';
import {
  addProjectCreatorToTemplate,
  bulkApplyTemplate,
  getPermissionTemplateGroups,
  getPermissionTemplates,
  getPermissionTemplateUsers,
  grantTemplatePermissionToGroup,
  grantTemplatePermissionToUser,
  removeProjectCreatorFromTemplate,
  revokeTemplatePermissionFromGroup,
  revokeTemplatePermissionFromUser,
} from '../permissions';

const MAX_PROJECTS_TO_APPLY_PERMISSION_TEMPLATE = 10;

const defaultPermissionTemplates: PermissionTemplate[] = [
  mockPermissionTemplate(),
  mockPermissionTemplate({
    id: 'template2',
    name: 'Permission Template 2',
  }),
];

const templateUsers = [
  mockTemplateUser(),
  mockTemplateUser({
    login: 'gooduser1',
    name: 'John',
    permissions: ['issueadmin', 'securityhotspotadmin', 'user'],
  }),
  mockTemplateUser({
    login: 'gooduser2',
    name: 'Alexa',
    permissions: ['issueadmin', 'user'],
  }),
  mockTemplateUser({
    name: 'Siri',
    login: 'gooduser3',
  }),
  mockTemplateUser({
    login: 'gooduser4',
    name: 'Cool',
    permissions: ['user'],
  }),
  mockTemplateUser({
    name: 'White',
    login: 'baduser1',
  }),
  mockTemplateUser({
    name: 'Green',
    login: 'baduser2',
  }),
];

const templateGroups = [
  mockTemplateGroup(),
  mockTemplateGroup({ id: 'admins', name: 'admins', permissions: [] }),
];

const PAGE_SIZE = 5;
const MIN_QUERY_LENGTH = 3;
const DEFAULT_PAGE = 1;

jest.mock('../permissions');

export default class PermissionTemplateServiceMock {
  permissionTemplates: PermissionTemplate[] = [];
  isAllowedPermissionChange = true;

  constructor() {
    this.permissionTemplates = cloneDeep(defaultPermissionTemplates);
    (getPermissionTemplates as jest.Mock).mockImplementation(this.handleGetPermissionTemplates);
    (bulkApplyTemplate as jest.Mock).mockImplementation(this.handleBulkApplyTemplate);
    (getPermissionTemplateUsers as jest.Mock).mockImplementation(
      this.handleGetPermissionTemplateUsers
    );
    (getPermissionTemplateGroups as jest.Mock).mockImplementation(
      this.handleGetPermissionTemplateGroups
    );
    (addProjectCreatorToTemplate as jest.Mock).mockImplementation(this.handlePermissionChange);
    (removeProjectCreatorFromTemplate as jest.Mock).mockImplementation(this.handlePermissionChange);
    (grantTemplatePermissionToGroup as jest.Mock).mockImplementation(this.handlePermissionChange);
    (revokeTemplatePermissionFromGroup as jest.Mock).mockImplementation(
      this.handlePermissionChange
    );
    (grantTemplatePermissionToUser as jest.Mock).mockImplementation(this.handlePermissionChange);
    (revokeTemplatePermissionFromUser as jest.Mock).mockImplementation(this.handlePermissionChange);
  }

  handleGetPermissionTemplates = () => {
    return this.reply({ permissionTemplates: this.permissionTemplates });
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

  handleGetPermissionTemplateUsers = (data: { q?: string | null; p?: number; ps?: number }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q } = data;

    const users =
      q && q.length >= MIN_QUERY_LENGTH
        ? templateUsers.filter((user) =>
            [user.login, user.name].some((key) => key.toLowerCase().includes(q.toLowerCase()))
          )
        : templateUsers;

    const usersChunks = chunk(users, ps);

    return this.reply({
      paging: { pageSize: ps, total: users.length, pageIndex: p },
      users: usersChunks[p - 1] ?? [],
    });
  };

  handleGetPermissionTemplateGroups = (data: {
    templateId: string;
    q?: string | null;
    permission?: string;
    p?: number;
    ps?: number;
  }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q } = data;

    const groups =
      q && q.length >= MIN_QUERY_LENGTH
        ? templateGroups.filter((group) => group.name.toLowerCase().includes(q.toLowerCase()))
        : templateGroups;

    const groupsChunks = chunk(groups, ps);

    return this.reply({
      paging: { pageSize: ps, total: groups.length, pageIndex: p },
      groups: groupsChunks[p - 1] ?? [],
    });
  };

  handlePermissionChange = () => {
    return this.isAllowedPermissionChange ? Promise.resolve() : Promise.reject();
  };

  updatePermissionChangeAllowance = (val: boolean) => {
    this.isAllowedPermissionChange = val;
  };

  reset = () => {
    this.permissionTemplates = cloneDeep(defaultPermissionTemplates);
    this.updatePermissionChangeAllowance(true);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
