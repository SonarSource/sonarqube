/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { getJSON, post, postJSON } from '../helpers/request';

const PAGE_SIZE = 100;

export function grantPermissionToUser(
  projectKey: string | null,
  login: string,
  permission: string,
  organization?: string
) {
  const url = '/api/permissions/add_user';
  const data: Object = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization && !projectKey) {
    data.organization = organization;
  }
  return post(url, data);
}

export function revokePermissionFromUser(
  projectKey: string | null,
  login: string,
  permission: string,
  organization?: string
) {
  const url = '/api/permissions/remove_user';
  const data: Object = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization && !projectKey) {
    data.organization = organization;
  }
  return post(url, data);
}

export function grantPermissionToGroup(
  projectKey: string | null,
  groupName: string,
  permission: string,
  organization?: string
) {
  const url = '/api/permissions/add_group';
  const data: Object = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization) {
    data.organization = organization;
  }
  return post(url, data);
}

export function revokePermissionFromGroup(
  projectKey: string | null,
  groupName: string,
  permission: string,
  organization?: string
) {
  const url = '/api/permissions/remove_group';
  const data: Object = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization) {
    data.organization = organization;
  }
  return post(url, data);
}

/**
 * Get list of permission templates
 * @returns {Promise}
 */
export function getPermissionTemplates(organization?: string) {
  const url = '/api/permissions/search_templates';
  return organization ? getJSON(url, { organization }) : getJSON(url);
}

export const createPermissionTemplate = (data: Object) =>
  postJSON('/api/permissions/create_template', data);

export const updatePermissionTemplate = (data: Object) =>
  post('/api/permissions/update_template', data);

export const deletePermissionTemplate = (data: Object) =>
  post('/api/permissions/delete_template', data);

/**
 * Set default permission template for a given qualifier
 * @param {string} templateId
 * @param {string} qualifier
 * @returns {Promise}
 */
export function setDefaultPermissionTemplate(templateId: string, qualifier: string) {
  const url = '/api/permissions/set_default_template';
  const data = { templateId, qualifier };
  return post(url, data);
}

export function applyTemplateToProject(data: Object) {
  const url = '/api/permissions/apply_template';
  return post(url, data);
}

export function bulkApplyTemplate(data: Object) {
  const url = '/api/permissions/bulk_apply_template';
  return post(url, data);
}

export function grantTemplatePermissionToUser(
  data: {
    templateId: string,
    login: string,
    permission: string,
    organization?: string
  }
) {
  const url = '/api/permissions/add_user_to_template';
  return post(url, data);
}

export function revokeTemplatePermissionFromUser(
  data: {
    templateId: string,
    login: string,
    permission: string,
    organization?: string
  }
) {
  const url = '/api/permissions/remove_user_from_template';
  return post(url, data);
}

export function grantTemplatePermissionToGroup(data: Object) {
  const url = '/api/permissions/add_group_to_template';
  return post(url, data);
}

export function revokeTemplatePermissionFromGroup(data: Object) {
  const url = '/api/permissions/remove_group_from_template';
  return post(url, data);
}

export function addProjectCreatorToTemplate(templateId: string, permission: string) {
  const url = '/api/permissions/add_project_creator_to_template';
  const data = { templateId, permission };
  return post(url, data);
}

export function removeProjectCreatorFromTemplate(templateId: string, permission: string) {
  const url = '/api/permissions/remove_project_creator_from_template';
  const data = { templateId, permission };
  return post(url, data);
}

export function getPermissionsUsersForComponent(
  projectKey: string,
  query?: string,
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/users';
  const data: Object = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON(url, data).then(r => r.users);
}

export function getPermissionsGroupsForComponent(
  projectKey: string,
  query: string = '',
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/groups';
  const data: Object = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON(url, data).then(r => r.groups);
}

export function getGlobalPermissionsUsers(
  query?: string,
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/users';
  const data: Object = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON(url, data).then(r => r.users);
}

export function getGlobalPermissionsGroups(
  query?: string,
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/groups';
  const data: Object = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON(url, data).then(r => r.groups);
}

export function getPermissionTemplateUsers(
  templateId: string,
  query?: string,
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/template_users';
  const data: Object = { templateId, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    Object.assign(data, { organization });
  }
  return getJSON(url, data).then(r => r.users);
}

export function getPermissionTemplateGroups(
  templateId: string,
  query?: string,
  permission?: string,
  organization?: string
) {
  const url = '/api/permissions/template_groups';
  const data: Object = { templateId, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    Object.assign(data, { organization });
  }
  return getJSON(url, data).then(r => r.groups);
}

export function changeProjectVisibility(project: string, visibility: string): Promise<void> {
  const url = '/api/projects/update_visibility';
  const data = { project, visibility };
  return post(url, data);
}
