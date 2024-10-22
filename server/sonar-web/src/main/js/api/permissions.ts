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

import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { Visibility } from '~sonar-aligned/types/component';
import { post, postJSON, RequestData } from '../helpers/request';
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
  login: string;
  permission: string;
  projectKey?: string;
}) {
  return post('/api/permissions/add_user', data).catch(throwGlobalError);
}

export function revokePermissionFromUser(data: {
  login: string;
  permission: string;
  projectKey?: string;
}) {
  return post('/api/permissions/remove_user', data).catch(throwGlobalError);
}

export function grantPermissionToGroup(data: {
  groupName: string;
  permission: string;
  projectKey?: string;
}) {
  return post('/api/permissions/add_group', data).catch(throwGlobalError);
}

export function revokePermissionFromGroup(data: {
  groupName: string;
  permission: string;
  projectKey?: string;
}) {
  return post('/api/permissions/remove_group', data).catch(throwGlobalError);
}

interface GetPermissionTemplatesResponse {
  defaultTemplates: Array<{ qualifier: string; templateId: string }>;
  permissionTemplates: PermissionTemplate[];
  permissions: Array<Permission>;
}

export function getPermissionTemplates(): Promise<GetPermissionTemplatesResponse> {
  const url = '/api/permissions/search_templates';
  return getJSON(url);
}

export function createPermissionTemplate(data: {
  description?: string;
  name: string;
  projectKeyPattern?: string;
}): Promise<{ permissionTemplate: Omit<PermissionTemplate, 'defaultFor'> }> {
  return postJSON('/api/permissions/create_template', data);
}

export function updatePermissionTemplate(data: {
  description?: string;
  id: string;
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
  login: string;
  permission: string;
  templateId: string;
}): Promise<void> {
  return post('/api/permissions/add_user_to_template', data);
}

export function revokeTemplatePermissionFromUser(data: {
  login: string;
  permission: string;
  templateId: string;
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
  p?: number;
  permission?: string;
  projectKey: string;
  ps?: number;
  q?: string;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data).catch(throwGlobalError);
}

export function getPermissionsGroupsForComponent(data: {
  p?: number;
  permission?: string;
  projectKey: string;
  ps?: number;
  q?: string;
}): Promise<{ groups: PermissionGroup[]; paging: Paging }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data).catch(throwGlobalError);
}

export function getGlobalPermissionsUsers(data: {
  p?: number;
  permission?: string;
  ps?: number;
  q?: string;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/users', data);
}

export function getGlobalPermissionsGroups(data: {
  p?: number;
  permission?: string;
  ps?: number;
  q?: string;
}): Promise<{ groups: PermissionGroup[]; paging: Paging }> {
  if (!data.ps) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/groups', data);
}

export function getPermissionTemplateUsers(data: {
  p?: number;
  permission?: string;
  ps?: number;
  q?: string;
  templateId: string;
}): Promise<{ paging: Paging; users: PermissionUser[] }> {
  if (data.ps === undefined) {
    data.ps = PAGE_SIZE;
  }
  return getJSON('/api/permissions/template_users', data).catch(throwGlobalError);
}

export function getPermissionTemplateGroups(data: {
  p?: number;
  permission?: string;
  ps?: number;
  q?: string;
  templateId: string;
}): Promise<{ groups: PermissionGroup[]; paging: Paging }> {
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
