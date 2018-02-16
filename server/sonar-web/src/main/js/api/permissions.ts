/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { BaseSearchProjectsParameters } from './components';
import { PermissionTemplate } from '../app/types';
import throwGlobalError from '../app/utils/throwGlobalError';
import { getJSON, post, postJSON, RequestData } from '../helpers/request';

const PAGE_SIZE = 100;

export function grantPermissionToUser(
  projectKey: string | null,
  login: string,
  permission: string,
  organization?: string
): Promise<void> {
  const data: RequestData = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization && !projectKey) {
    data.organization = organization;
  }
  return post('/api/permissions/add_user', data);
}

export function revokePermissionFromUser(
  projectKey: string | null,
  login: string,
  permission: string,
  organization?: string
): Promise<void> {
  const data: RequestData = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization && !projectKey) {
    data.organization = organization;
  }
  return post('/api/permissions/remove_user', data);
}

export function grantPermissionToGroup(
  projectKey: string | null,
  groupName: string,
  permission: string,
  organization?: string
): Promise<void> {
  const data: RequestData = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization) {
    data.organization = organization;
  }
  return post('/api/permissions/add_group', data);
}

export function revokePermissionFromGroup(
  projectKey: string | null,
  groupName: string,
  permission: string,
  organization?: string
): Promise<void> {
  const data: RequestData = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  if (organization) {
    data.organization = organization;
  }
  return post('/api/permissions/remove_group', data);
}

interface GetPermissionTemplatesResponse {
  permissionTemplates: PermissionTemplate[];
  defaultTemplates: Array<{ templateId: string; qualifier: string }>;
  permissions: Array<{ key: string; name: string; description: string }>;
}

export function getPermissionTemplates(
  organization?: string
): Promise<GetPermissionTemplatesResponse> {
  const url = '/api/permissions/search_templates';
  return organization ? getJSON(url, { organization }) : getJSON(url);
}

export function createPermissionTemplate(data: RequestData) {
  return postJSON('/api/permissions/create_template', data);
}

export function updatePermissionTemplate(data: RequestData): Promise<void> {
  return post('/api/permissions/update_template', data);
}

export function deletePermissionTemplate(data: RequestData) {
  return post('/api/permissions/delete_template', data).catch(throwGlobalError);
}

/**
 * Set default permission template for a given qualifier
 */
export function setDefaultPermissionTemplate(templateId: string, qualifier: string): Promise<void> {
  return post('/api/permissions/set_default_template', { templateId, qualifier });
}

export function applyTemplateToProject(data: RequestData) {
  return post('/api/permissions/apply_template', data).catch(throwGlobalError);
}

export function bulkApplyTemplate(data: BaseSearchProjectsParameters): Promise<void> {
  return post('/api/permissions/bulk_apply_template', data);
}

export function grantTemplatePermissionToUser(data: {
  templateId: string;
  login: string;
  permission: string;
  organization?: string;
}): Promise<void> {
  return post('/api/permissions/add_user_to_template', data);
}

export function revokeTemplatePermissionFromUser(data: {
  templateId: string;
  login: string;
  permission: string;
  organization?: string;
}): Promise<void> {
  return post('/api/permissions/remove_user_from_template', data);
}

export function grantTemplatePermissionToGroup(data: RequestData): Promise<void> {
  return post('/api/permissions/add_group_to_template', data);
}

export function revokeTemplatePermissionFromGroup(data: RequestData): Promise<void> {
  return post('/api/permissions/remove_group_from_template', data);
}

export function addProjectCreatorToTemplate(templateId: string, permission: string): Promise<void> {
  return post('/api/permissions/add_project_creator_to_template', { templateId, permission });
}

export function removeProjectCreatorFromTemplate(
  templateId: string,
  permission: string
): Promise<void> {
  return post('/api/permissions/remove_project_creator_from_template', { templateId, permission });
}

export interface PermissionUser {
  login: string;
  name: string;
  email?: string;
  permissions: string[];
  avatar?: string;
}

export function getPermissionsUsersForComponent(
  projectKey: string,
  query?: string,
  permission?: string,
  organization?: string
): Promise<PermissionUser[]> {
  const data: RequestData = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/permissions/users', data).then(r => r.users);
}

export interface PermissionGroup {
  id: string;
  name: string;
  description?: string;
  permissions: string[];
}

export function getPermissionsGroupsForComponent(
  projectKey: string,
  query: string = '',
  permission?: string,
  organization?: string
): Promise<PermissionGroup[]> {
  const data: RequestData = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/permissions/groups', data).then(r => r.groups);
}

export function getGlobalPermissionsUsers(
  query?: string,
  permission?: string,
  organization?: string
): Promise<PermissionUser[]> {
  const data: RequestData = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/permissions/users', data).then(r => r.users);
}

export function getGlobalPermissionsGroups(
  query?: string,
  permission?: string,
  organization?: string
): Promise<PermissionGroup[]> {
  const data: RequestData = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    data.organization = organization;
  }
  return getJSON('/api/permissions/groups', data).then(r => r.groups);
}

export function getPermissionTemplateUsers(
  templateId: string,
  query?: string,
  permission?: string,
  organization?: string
): Promise<any> {
  const data: RequestData = { templateId, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    Object.assign(data, { organization });
  }
  return getJSON('/api/permissions/template_users', data).then(r => r.users);
}

export function getPermissionTemplateGroups(
  templateId: string,
  query?: string,
  permission?: string,
  organization?: string
): Promise<any> {
  const data: RequestData = { templateId, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  if (organization) {
    Object.assign(data, { organization });
  }
  return getJSON('/api/permissions/template_groups', data).then(r => r.groups);
}

export function changeProjectVisibility(project: string, visibility: string): Promise<void> {
  return post('/api/projects/update_visibility', { project, visibility });
}
