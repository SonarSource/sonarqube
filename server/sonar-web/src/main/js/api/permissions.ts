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
import { throwGlobalError } from '../helpers/error';
import { getJSON, post, postJSON, RequestData } from '../helpers/request';
import { Visibility } from '../types/component';
import {
  Paging,
  Permission,
  PermissionGroup,
  PermissionTemplate,
  PermissionUser,
} from '../types/types';
import { BaseSearchProjectsParameters } from './components';

const PAGE_SIZE = 100;

export function grantPermissionToUser(data: {
  projectKey?: string;
  login: string;
  permission: string;
}) {
  return post('/api/permissions/add_user', data).catch(throwGlobalError);
}

export function revokePermissionFromUser(data: {
  projectKey?: string;
  login: string;
  permission: string;
}) {
  return post('/api/permissions/remove_user', data).catch(throwGlobalError);
}

export function grantPermissionToGroup(data: {
  projectKey?: string;
  groupName: string;
  permission: string;
}) {
  return post('/api/permissions/add_group', data).catch(throwGlobalError);
}

export function revokePermissionFromGroup(data: {
  projectKey?: string;
  groupName: string;
  permission: string;
}) {
  return post('/api/permissions/remove_group', data).catch(throwGlobalError);
}

interface GetPermissionTemplatesResponse {
  permissionTemplates: PermissionTemplate[];
  defaultTemplates: Array<{ templateId: string; qualifier: string }>;
  permissions: Array<Permission>;
}

export function getPermissionTemplates(): Promise<GetPermissionTemplatesResponse> {
  const url = '/api/permissions/search_templates';
  return getJSON(url);
}

export function createPermissionTemplate(data: {
  name: string;
  description?: string;
  projectKeyPattern?: string;
}): Promise<{ permissionTemplate: Omit<PermissionTemplate, 'defaultFor'> }> {
  return postJSON('/api/permissions/create_template', data);
}

export function updatePermissionTemplate(data: {
  id: string;
  description?: string;
  name?: string;
  projectKeyPattern?: string;
}): Promise<void> {
  return post('/api/permissions/update_template', data);
}

export function deletePermissionTemplate(data: { templateId?: string; templateName?: string }) {
  return post('/api/permissions/delete_template', data).catch(throwGlobalError);
}

/**
 * Set default permission template for a given qualifier
 */
export function setDefaultPermissionTemplate(templateId: string, qualifier: string): Promise<void> {
  return post('/api/permissions/set_default_template', { templateId, qualifier });
}

export function applyTemplateToProject(data: { projectKey: string; templateId: string }) {
  return post('/api/permissions/apply_template', data).catch(throwGlobalError);
}

export function bulkApplyTemplate(data: BaseSearchProjectsParameters): Promise<void> {
  return post('/api/permissions/bulk_apply_template', data);
}

export function grantTemplatePermissionToUser(data: {
  templateId: string;
  login: string;
  permission: string;
}): Promise<void> {
  return post('/api/permissions/add_user_to_template', data);
}

export function revokeTemplatePermissionFromUser(data: {
  templateId: string;
  login: string;
  permission: string;
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
  permission: string,
): Promise<void> {
  return post('/api/permissions/remove_project_creator_from_template', { templateId, permission });
}

export function getPermissionsUsersForComponent(data: {
  projectKey: string;
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data).catch(throwGlobalError);
}

export function getPermissionsGroupsForComponent(data: {
  projectKey: string;
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; groups: PermissionGroup[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data).catch(throwGlobalError);
}

export function getGlobalPermissionsUsers(data: {
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data);
}

export function getGlobalPermissionsGroups(data: {
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; groups: PermissionGroup[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data);
}

export function getPermissionTemplateUsers(data: {
  templateId: string;
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (data.ps === undefined) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/template_users', data).catch(throwGlobalError);
}

export function getPermissionTemplateGroups(data: {
  templateId: string;
  q?: string;
  permission?: string;
  p?: number;
  ps?: number;
}): Promise<{ paging: Paging; groups: PermissionGroup[] }> {
  if (data.ps === undefined) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/template_groups', data).catch(throwGlobalError);
}

export function changeProjectVisibility(
  project: string,
  visibility: Visibility,
): Promise<void | Response> {
  return post('/api/projects/update_visibility', { project, visibility }).catch(throwGlobalError);
}
