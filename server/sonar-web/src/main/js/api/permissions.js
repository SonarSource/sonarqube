/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import _ from 'underscore';
import { getJSON, post } from '../helpers/request';

const PAGE_SIZE = 100;

function request (options) {
  return $.ajax(options);
}

export function getPermissionUsers (data) {
  const url = '/api/permissions/users';
  return getJSON(url, data);
}

export function grantPermissionToUser (projectKey, login, permission) {
  const url = '/api/permissions/add_user';
  const data = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  return post(url, data);
}

export function revokePermissionFromUser (projectKey, login, permission) {
  const url = '/api/permissions/remove_user';
  const data = { login, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  return post(url, data);
}

export function getPermissionGroups (data) {
  const url = '/api/permissions/groups';
  return getJSON(url, data);
}

export function grantPermissionToGroup (projectKey, groupName, permission) {
  const url = '/api/permissions/add_group';
  const data = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  return post(url, data);
}

export function revokePermissionFromGroup (projectKey, groupName, permission) {
  const url = '/api/permissions/remove_group';
  const data = { groupName, permission };
  if (projectKey) {
    data.projectKey = projectKey;
  }
  return post(url, data);
}

/**
 * Get list of permission templates
 * @returns {Promise}
 */
export function getPermissionTemplates () {
  const url = '/api/permissions/search_templates';
  return getJSON(url);
}

export function createPermissionTemplate (options) {
  const url = window.baseUrl + '/api/permissions/create_template';
  return request(_.extend({ type: 'POST', url }, options));
}

export function updatePermissionTemplate (options) {
  const url = window.baseUrl + '/api/permissions/update_template';
  return request(_.extend({ type: 'POST', url }, options));
}

export function deletePermissionTemplate (options) {
  const url = window.baseUrl + '/api/permissions/delete_template';
  return request(_.extend({ type: 'POST', url }, options));
}

/**
 * Set default permission template for a given qualifier
 * @param {string} templateName
 * @param {string} qualifier
 * @returns {Promise}
 */
export function setDefaultPermissionTemplate (templateName, qualifier) {
  const url = '/api/permissions/set_default_template';
  const data = { templateName, qualifier };
  return post(url, data);
}

export function applyTemplateToProject (data) {
  const url = '/api/permissions/apply_template';
  return post(url, data);
}

export function bulkApplyTemplate (data) {
  const url = '/api/permissions/bulk_apply_template';
  return post(url, data);
}

export function addProjectCreatorToTemplate (templateName, permission) {
  const url = '/api/permissions/add_project_creator_to_template';
  const data = { templateName, permission };
  return post(url, data);
}

export function removeProjectCreatorFromTemplate (templateName, permission) {
  const url = '/api/permissions/remove_project_creator_from_template';
  const data = { templateName, permission };
  return post(url, data);
}

export function getPermissionsUsersForComponent (projectKey, query = '', permission = null) {
  const url = '/api/permissions/users';
  const data = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  return getJSON(url, data).then(r => r.users);
}

export function getPermissionsGroupsForComponent (projectKey, query = '', permission = null) {
  const url = '/api/permissions/groups';
  const data = { projectKey, ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  return getJSON(url, data).then(r => r.groups);
}

export function getGlobalPermissionsUsers (query = '', permission = null) {
  const url = '/api/permissions/users';
  const data = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  return getJSON(url, data).then(r => r.users);
}

export function getGlobalPermissionsGroups (query = '', permission = null) {
  const url = '/api/permissions/groups';
  const data = { ps: PAGE_SIZE };
  if (query) {
    data.q = query;
  }
  if (permission) {
    data.permission = permission;
  }
  return getJSON(url, data).then(r => r.groups);
}
