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
import { chunk, cloneDeep, remove, uniq } from 'lodash';
import {
  mockPermission,
  mockPermissionGroup,
  mockPermissionTemplate,
  mockPermissionUser,
  mockTemplateGroup,
  mockTemplateUser,
} from '../../helpers/mocks/permissions';
import { ComponentQualifier, Visibility } from '../../types/component';
import { Permissions } from '../../types/permissions';
import { Permission, PermissionGroup, PermissionTemplate, PermissionUser } from '../../types/types';
import { BaseSearchProjectsParameters } from '../components';
import {
  addProjectCreatorToTemplate,
  applyTemplateToProject,
  bulkApplyTemplate,
  changeProjectVisibility,
  getGlobalPermissionsGroups,
  getGlobalPermissionsUsers,
  getPermissionsGroupsForComponent,
  getPermissionsUsersForComponent,
  getPermissionTemplateGroups,
  getPermissionTemplates,
  getPermissionTemplateUsers,
  grantPermissionToGroup,
  grantPermissionToUser,
  grantTemplatePermissionToGroup,
  grantTemplatePermissionToUser,
  removeProjectCreatorFromTemplate,
  revokePermissionFromGroup,
  revokePermissionFromUser,
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

const defaultUsers = [mockPermissionUser()];

const defaultGroups = [
  mockPermissionGroup({ name: 'sonar-users', permissions: [Permissions.Browse] }),
  mockPermissionGroup({
    name: 'sonar-admins',
    permissions: [Permissions.Admin, Permissions.Browse],
  }),
  mockPermissionGroup({ name: 'sonar-losers', permissions: [] }),
];

const PAGE_SIZE = 5;
const MIN_QUERY_LENGTH = 3;
const DEFAULT_PAGE = 1;

jest.mock('../permissions');

export default class PermissionsServiceMock {
  #permissionTemplates: PermissionTemplate[] = [];
  #permissions: Permission[];
  #defaultTemplates: Array<{ templateId: string; qualifier: string }>;
  #groups: PermissionGroup[];
  #users: PermissionUser[];
  #isAllowedToChangePermissions = true;

  constructor() {
    this.#permissionTemplates = cloneDeep(defaultPermissionTemplates);
    this.#defaultTemplates = [
      ComponentQualifier.Project,
      ComponentQualifier.Application,
      ComponentQualifier.Portfolio,
    ].map((qualifier) => ({ templateId: this.#permissionTemplates[0].id, qualifier }));
    this.#permissions = [
      Permissions.Admin,
      Permissions.CodeViewer,
      Permissions.IssueAdmin,
      Permissions.SecurityHotspotAdmin,
      Permissions.Scan,
      Permissions.Browse,
    ].map((key) => mockPermission({ key, name: key }));
    this.#groups = cloneDeep(defaultGroups);
    this.#users = cloneDeep(defaultUsers);

    jest.mocked(getPermissionTemplates).mockImplementation(this.handleGetPermissionTemplates);
    jest.mocked(bulkApplyTemplate).mockImplementation(this.handleBulkApplyTemplate);
    jest.mocked(applyTemplateToProject).mockImplementation(this.handleApplyTemplateToProject);
    jest
      .mocked(getPermissionTemplateUsers)
      .mockImplementation(this.handleGetPermissionTemplateUsers);
    jest
      .mocked(getPermissionTemplateGroups)
      .mockImplementation(this.handleGetPermissionTemplateGroups);
    jest.mocked(addProjectCreatorToTemplate).mockImplementation(this.handlePermissionChange);
    jest.mocked(removeProjectCreatorFromTemplate).mockImplementation(this.handlePermissionChange);
    jest.mocked(grantTemplatePermissionToGroup).mockImplementation(this.handlePermissionChange);
    jest.mocked(revokeTemplatePermissionFromGroup).mockImplementation(this.handlePermissionChange);
    jest.mocked(grantTemplatePermissionToUser).mockImplementation(this.handlePermissionChange);
    jest.mocked(revokeTemplatePermissionFromUser).mockImplementation(this.handlePermissionChange);
    jest.mocked(changeProjectVisibility).mockImplementation(this.handleChangeProjectVisibility);
    jest.mocked(getGlobalPermissionsUsers).mockImplementation(this.handleGetPermissionUsers);
    jest.mocked(getGlobalPermissionsGroups).mockImplementation(this.handleGetPermissionGroups);
    jest
      .mocked(getPermissionsGroupsForComponent)
      .mockImplementation(this.handleGetPermissionGroupsForComponent);
    jest
      .mocked(getPermissionsUsersForComponent)
      .mockImplementation(this.handleGetPermissionUsersForComponent);
    jest.mocked(grantPermissionToGroup).mockImplementation(this.handleGrantPermissionToGroup);
    jest.mocked(revokePermissionFromGroup).mockImplementation(this.handleRevokePermissionFromGroup);
    jest.mocked(grantPermissionToUser).mockImplementation(this.handleGrantPermissionToUser);
    jest.mocked(revokePermissionFromUser).mockImplementation(this.handleRevokePermissionFromUser);
  }

  handleGetPermissionTemplates = () => {
    return this.reply({
      permissionTemplates: this.#permissionTemplates,
      defaultTemplates: this.#defaultTemplates,
      permissions: this.#permissions,
    });
  };

  handleApplyTemplateToProject = (_data: { projectKey: string; templateId: string }) => {
    return this.reply(undefined);
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

  handleChangeProjectVisibility = (_project: string, _visibility: Visibility) => {
    return this.reply(undefined);
  };

  handleGetPermissionUsers = (data: {
    q?: string;
    permission?: string;
    p?: number;
    ps?: number;
  }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q, permission } = data;

    const users =
      q && q.length >= MIN_QUERY_LENGTH
        ? this.#users.filter((user) => user.name.toLowerCase().includes(q.toLowerCase()))
        : this.#users;

    const usersChunked = chunk(
      permission ? users.filter((u) => u.permissions.includes(permission)) : users,
      ps
    );

    return this.reply({
      paging: { pageSize: ps, total: users.length, pageIndex: p },
      users: usersChunked[p - 1] ?? [],
    });
  };

  handleGetPermissionGroups = (data: {
    q?: string;
    permission?: string;
    p?: number;
    ps?: number;
  }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q, permission } = data;

    const groups =
      q && q.length >= MIN_QUERY_LENGTH
        ? this.#groups.filter((group) => group.name.toLowerCase().includes(q.toLowerCase()))
        : this.#groups;

    const groupsChunked = chunk(
      permission ? groups.filter((g) => g.permissions.includes(permission)) : groups,
      ps
    );

    return this.reply({
      paging: { pageSize: ps, total: groups.length, pageIndex: p },
      groups: groupsChunked[p - 1] ?? [],
    });
  };

  handleGetPermissionGroupsForComponent = (data: {
    projectKey: string;
    q?: string;
    permission?: string;
    p?: number;
    ps?: number;
  }) => {
    return this.handleGetPermissionGroups(data);
  };

  handleGetPermissionUsersForComponent = (data: {
    projectKey: string;
    q?: string;
    permission?: string;
    p?: number;
    ps?: number;
  }) => {
    return this.handleGetPermissionUsers(data);
  };

  handleGrantPermissionToGroup = (data: {
    projectKey?: string;
    groupName: string;
    permission: string;
  }) => {
    if (!this.#isAllowedToChangePermissions) {
      return Promise.reject();
    }

    const { groupName, permission } = data;
    const group = this.#groups.find((g) => g.name === groupName);
    if (group === undefined) {
      throw new Error(`Could not find group with name ${groupName}`);
    }
    group.permissions = uniq([...group.permissions, permission]);
    return this.reply(undefined);
  };

  handleRevokePermissionFromGroup = (data: {
    projectKey?: string;
    groupName: string;
    permission: string;
  }) => {
    if (!this.#isAllowedToChangePermissions) {
      return Promise.reject();
    }

    const { groupName, permission } = data;
    const group = this.#groups.find((g) => g.name === groupName);
    if (group === undefined) {
      throw new Error(`Could not find group with name ${groupName}`);
    }
    group.permissions = remove(group.permissions, permission);
    return this.reply(undefined);
  };

  handleGrantPermissionToUser = (data: {
    projectKey?: string;
    login: string;
    permission: string;
  }) => {
    if (!this.#isAllowedToChangePermissions) {
      return Promise.reject();
    }

    const { login, permission } = data;
    const user = this.#users.find((u) => u.login === login);
    if (user === undefined) {
      throw new Error(`Could not find user with login ${login}`);
    }
    user.permissions = uniq([...user.permissions, permission]);
    return this.reply(undefined);
  };

  handleRevokePermissionFromUser = (data: {
    projectKey?: string;
    login: string;
    permission: string;
  }) => {
    if (!this.#isAllowedToChangePermissions) {
      return Promise.reject();
    }

    const { login, permission } = data;
    const user = this.#users.find((u) => u.login === login);
    if (user === undefined) {
      throw new Error(`Could not find user with name ${login}`);
    }
    user.permissions = remove(user.permissions, permission);
    return this.reply(undefined);
  };

  handlePermissionChange = () => {
    return this.#isAllowedToChangePermissions ? Promise.resolve() : Promise.reject();
  };

  setIsAllowedToChangePermissions = (val: boolean) => {
    this.#isAllowedToChangePermissions = val;
  };

  setGroups = (groups: PermissionGroup[]) => {
    this.#groups = groups;
  };

  setUsers = (users: PermissionUser[]) => {
    this.#users = users;
  };

  reset = () => {
    this.#permissionTemplates = cloneDeep(defaultPermissionTemplates);
    this.#groups = cloneDeep(defaultGroups);
    this.#users = cloneDeep(defaultUsers);
    this.setIsAllowedToChangePermissions(true);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
