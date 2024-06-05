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
import { chunk, cloneDeep, remove, uniq } from 'lodash';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import {
  mockPermission,
  mockPermissionGroup,
  mockPermissionTemplate,
  mockPermissionTemplateGroup,
  mockPermissionUser,
} from '../../helpers/mocks/permissions';
import { PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE } from '../../helpers/permissions';
import { Permissions } from '../../types/permissions';
import { Permission, PermissionGroup, PermissionTemplate, PermissionUser } from '../../types/types';
import { BaseSearchProjectsParameters } from '../components';
import {
  addProjectCreatorToTemplate,
  applyTemplateToProject,
  bulkApplyTemplate,
  changeProjectVisibility,
  createPermissionTemplate,
  deletePermissionTemplate,
  getGlobalPermissionsGroups,
  getGlobalPermissionsUsers,
  getPermissionTemplateGroups,
  getPermissionTemplateUsers,
  getPermissionTemplates,
  getPermissionsGroupsForComponent,
  getPermissionsUsersForComponent,
  grantPermissionToGroup,
  grantPermissionToUser,
  grantTemplatePermissionToGroup,
  grantTemplatePermissionToUser,
  removeProjectCreatorFromTemplate,
  revokePermissionFromGroup,
  revokePermissionFromUser,
  revokeTemplatePermissionFromGroup,
  revokeTemplatePermissionFromUser,
  setDefaultPermissionTemplate,
  updatePermissionTemplate,
} from '../permissions';

const MAX_PROJECTS_TO_APPLY_PERMISSION_TEMPLATE = 10;

const defaultUsers = [
  mockPermissionUser(),
  mockPermissionUser({
    login: 'gooduser1',
    name: 'John',
    managed: true,
    permissions: [
      Permissions.IssueAdmin,
      Permissions.SecurityHotspotAdmin,
      Permissions.Browse,
      Permissions.Admin,
    ],
  }),
  mockPermissionUser({
    login: 'gooduser2',
    name: 'Alexa',
    permissions: [Permissions.IssueAdmin, Permissions.Browse],
  }),
  mockPermissionUser({
    name: 'Siri',
    login: 'gooduser3',
    managed: true,
  }),
  mockPermissionUser({
    login: 'gooduser4',
    name: 'Cool',
    permissions: [Permissions.Browse],
  }),
  mockPermissionUser({
    name: 'White',
    login: 'baduser1',
  }),
  mockPermissionUser({
    name: 'Green',
    login: 'baduser2',
  }),
];

const defaultGroups = [
  mockPermissionGroup({ name: 'sonar-users', permissions: [Permissions.Browse] }),
  mockPermissionGroup({
    name: 'sonar-admins',
    managed: true,
    permissions: [Permissions.Admin, Permissions.Browse],
  }),
  mockPermissionGroup({ name: 'sonar-losers', permissions: [] }),
];

const defaultTemplates: PermissionTemplate[] = [
  mockPermissionTemplate({
    id: 'template1',
    name: 'Permission Template 1',
    description: 'This is permission template 1',
    defaultFor: [
      ComponentQualifier.Project,
      ComponentQualifier.Application,
      ComponentQualifier.Portfolio,
    ],
    permissions: PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE.map((key) =>
      mockPermissionTemplateGroup({
        key,
        groupsCount: defaultGroups.filter((g) => g.permissions.includes(key)).length,
        usersCount: defaultUsers.filter((g) => g.permissions.includes(key)).length,
        withProjectCreator: false,
      }),
    ),
  }),
  mockPermissionTemplate({
    id: 'template2',
    name: 'Permission Template 2',
    permissions: PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE.map((key) =>
      mockPermissionTemplateGroup({
        key,
        groupsCount: 0,
        usersCount: 0,
        withProjectCreator: [Permissions.Browse, Permissions.CodeViewer].includes(key),
      }),
    ),
  }),
];

const PAGE_SIZE = 5;
const MIN_QUERY_LENGTH = 3;
const DEFAULT_PAGE = 1;

jest.mock('../permissions');

export default class PermissionsServiceMock {
  #permissionTemplates: PermissionTemplate[] = [];
  #permissions: Permission[];
  #defaultTemplates: Array<{ qualifier: string; templateId: string }> = [];
  #groups: PermissionGroup[];
  #users: PermissionUser[];
  #isAllowedToChangePermissions = true;

  constructor() {
    this.#permissionTemplates = cloneDeep(defaultTemplates);
    this.#permissions = PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE.map((key) =>
      mockPermission({ key, name: key }),
    );
    this.#groups = cloneDeep(defaultGroups);
    this.#users = cloneDeep(defaultUsers);
    this.updateDefaults();

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
    jest.mocked(createPermissionTemplate).mockImplementation(this.handleCreatePermissionTemplate);
    jest.mocked(updatePermissionTemplate).mockImplementation(this.handleUpdatePermissionTemplate);
    jest.mocked(deletePermissionTemplate).mockImplementation(this.handleDeletePermissionTemplate);
    jest
      .mocked(setDefaultPermissionTemplate)
      .mockImplementation(this.handleSetDefaultPermissionTemplate);
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
        JSON.stringify({ errors: [{ msg: 'bulk apply permission template error message' }] }),
      );
      return Promise.reject(response);
    }

    return Promise.resolve();
  };

  handleGetPermissionTemplateUsers = (data: {
    p?: number;
    permission?: string;
    ps?: number;
    q?: string;
    templateId: string;
  }) => {
    return this.handleGetPermissionUsers(data);
  };

  handleGetPermissionTemplateGroups = (data: {
    p?: number;
    permission?: string;
    ps?: number;
    q?: string;
    templateId: string;
  }) => {
    return this.handleGetPermissionGroups(data);
  };

  handleChangeProjectVisibility = (_project: string, _visibility: Visibility) => {
    return this.reply(undefined);
  };

  handleGetPermissionUsers = (data: {
    p?: number;
    permission?: string;
    ps?: number;
    q?: string;
  }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q, permission } = data;

    const users =
      q && q.length >= MIN_QUERY_LENGTH
        ? this.#users.filter(
            (user) =>
              user.name.toLowerCase().includes(q.toLowerCase()) ||
              user.login.toLowerCase().includes(q.toLowerCase()),
          )
        : this.#users;

    const usersChunked = chunk(
      permission ? users.filter((u) => u.permissions.includes(permission)) : users,
      ps,
    );

    return this.reply({
      paging: { pageSize: ps, total: users.length, pageIndex: p },
      users: usersChunked[p - 1] ?? [],
    });
  };

  handleGetPermissionGroups = (data: {
    p?: number;
    permission?: string;
    ps?: number;
    q?: string;
  }) => {
    const { ps = PAGE_SIZE, p = DEFAULT_PAGE, q, permission } = data;

    const groups =
      q && q.length >= MIN_QUERY_LENGTH
        ? this.#groups.filter((group) => group.name.toLowerCase().includes(q.toLowerCase()))
        : this.#groups;

    const groupsChunked = chunk(
      permission ? groups.filter((g) => g.permissions.includes(permission)) : groups,
      ps,
    );

    return this.reply({
      paging: { pageSize: ps, total: groups.length, pageIndex: p },
      groups: groupsChunked[p - 1] ?? [],
    });
  };

  handleGetPermissionGroupsForComponent = (data: {
    p?: number;
    permission?: string;
    projectKey: string;
    ps?: number;
    q?: string;
  }) => {
    return this.handleGetPermissionGroups(data);
  };

  handleGetPermissionUsersForComponent = (data: {
    p?: number;
    permission?: string;
    projectKey: string;
    ps?: number;
    q?: string;
  }) => {
    return this.handleGetPermissionUsers(data);
  };

  handleGrantPermissionToGroup = (data: {
    groupName: string;
    permission: string;
    projectKey?: string;
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
    groupName: string;
    permission: string;
    projectKey?: string;
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
    login: string;
    permission: string;
    projectKey?: string;
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
    login: string;
    permission: string;
    projectKey?: string;
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

  handleCreatePermissionTemplate = (data: {
    description?: string;
    name: string;
    projectKeyPattern?: string;
  }) => {
    const newTemplate = mockPermissionTemplate({
      id: `template-${this.#permissionTemplates.length + 1}`,
      ...data,
    });
    this.#permissionTemplates.push(newTemplate);
    return this.reply({ permissionTemplate: newTemplate });
  };

  handleUpdatePermissionTemplate = (data: {
    description?: string;
    id: string;
    name?: string;
    projectKeyPattern?: string;
  }) => {
    const { id } = data;
    const template = this.#permissionTemplates.find((t) => t.id === id);
    if (template === undefined) {
      throw new Error(`Couldn't find template with id ${id}`);
    }
    Object.assign(template, data);

    return this.reply(undefined);
  };

  handleDeletePermissionTemplate = (data: { templateId?: string; templateName?: string }) => {
    const { templateId } = data;
    this.#permissionTemplates = this.#permissionTemplates.filter((t) => t.id !== templateId);
    return this.reply(undefined);
  };

  handleSetDefaultPermissionTemplate = (templateId: string, qualifier: ComponentQualifier) => {
    this.#permissionTemplates = this.#permissionTemplates.map((t) => ({
      ...t,
      defaultFor: t.defaultFor.filter((q) => q !== qualifier),
    }));

    const template = this.#permissionTemplates.find((t) => t.id === templateId);
    if (template === undefined) {
      throw new Error(`Couldn't find template with id ${templateId}`);
    }
    template.defaultFor = uniq([...template.defaultFor, qualifier]);

    this.updateDefaults();

    return this.reply(undefined);
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

  getTemplates = () => {
    return this.#permissionTemplates;
  };

  updateDefaults = () => {
    this.#defaultTemplates = [
      ComponentQualifier.Project,
      ComponentQualifier.Application,
      ComponentQualifier.Portfolio,
    ].map((qualifier) => ({
      templateId:
        this.#permissionTemplates.find((t) => t.defaultFor.includes(qualifier))?.id ??
        this.#permissionTemplates[0].id,
      qualifier,
    }));
  };

  reset = () => {
    this.#permissionTemplates = cloneDeep(defaultTemplates);
    this.#groups = cloneDeep(defaultGroups);
    this.#users = cloneDeep(defaultUsers);
    this.setIsAllowedToChangePermissions(true);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
