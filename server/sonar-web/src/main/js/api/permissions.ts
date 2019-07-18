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
import { getJSON, post, postJSON, RequestData } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import { BaseSearchProjectsParameters } from './components';

const PAGE_SIZE = 100;

export function grantPermissionToUser(data: {
  projectKey?: string;
  login: string;
  permission: string;
  organization?: string;
}) {
  return post('/api/permissions/add_user', data).catch(throwGlobalError);
}

export function revokePermissionFromUser(data: {
  projectKey?: string;
  login: string;
  permission: string;
  organization?: string;
}) {
  return post('/api/permissions/remove_user', data).catch(throwGlobalError);
}

export function grantPermissionToGroup(data: {
  projectKey?: string;
  groupName: string;
  permission: string;
  organization?: string;
}) {
  return post('/api/permissions/add_group', data).catch(throwGlobalError);
}

export function revokePermissionFromGroup(data: {
  projectKey?: string;
  groupName: string;
  permission: string;
  organization?: string;
}) {
  return post('/api/permissions/remove_group', data).catch(throwGlobalError);
}

interface GetPermissionTemplatesResponse {
  permissionTemplates: T.PermissionTemplate[];
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

export function getPermissionsUsersForComponent(data: {
  projectKey: string;
  q?: string;
  permission?: string;
  organization?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: T.Paging; users: T.PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data).catch(throwGlobalError);
}

export function getPermissionsGroupsForComponent(data: {
  projectKey: string;
  q?: string;
  permission?: string;
  organization?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: T.Paging; groups: T.PermissionGroup[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data).catch(throwGlobalError);
}

export function getGlobalPermissionsUsers(data: {
  q?: string;
  permission?: string;
  organization?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: T.Paging; users: T.PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data);
}

export function getGlobalPermissionsGroups(data: {
  q?: string;
  permission?: string;
  organization?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: T.Paging; groups: T.PermissionGroup[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data);
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

export function changeProjectVisibility(
  project: string,
  visibility: T.Visibility
): Promise<void | Response> {
  return post('/api/projects/update_visibility', { project, visibility }).catch(throwGlobalError);
}

export function changeProjectDefaultVisibility(
  organization: string,
  projectVisibility: T.Visibility
): Promise<void | Response> {
  return post('/api/projects/update_default_visibility', { organization, projectVisibility }).catch(
    throwGlobalError
  );
}
