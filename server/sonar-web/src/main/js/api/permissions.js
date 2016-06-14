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

function request (options) {
  return $.ajax(options);
}

function typeError (method, message) {
  throw new TypeError(`permissions#${method}: ${message}`);
}

export function getUsers (data) {
  const url = window.baseUrl + '/api/permissions/users';
  return request({ type: 'GET', url, data });
}

export function grantToUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('grantToUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return typeError('grantToUser', 'please provide user login');
  }

  const url = window.baseUrl + '/api/permissions/add_user';
  const data = { permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url, data });
}

export function revokeFromUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('revokeFromUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return typeError('revokeFromUser', 'please provide user login');
  }

  const url = window.baseUrl + '/api/permissions/remove_user';
  const data = { permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url, data });
}

export function getGroups (data) {
  const url = window.baseUrl + '/api/permissions/groups';
  return request({ type: 'GET', url, data });
}

export function grantToGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('grantToGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return typeError('grantToGroup', 'please provide group name');
  }

  const url = window.baseUrl + '/api/permissions/add_group';
  const data = { permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url, data });
}

export function revokeFromGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('revokeFromGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return typeError('revokeFromGroup', 'please provide group name');
  }

  const url = window.baseUrl + '/api/permissions/remove_group';
  const data = { permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url, data });
}

/**
 * Get list of permission templates
 * @returns {Promise}
 */
export function getPermissionTemplates () {
  const url = window.baseUrl + '/api/permissions/search_templates';
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
  const url = window.baseUrl + '/api/permissions/set_default_template';
  const data = { templateName, qualifier };
  return post(url, data);
}

export function applyTemplateToProject (options) {
  const url = window.baseUrl + '/api/permissions/apply_template';
  return request(_.extend({ type: 'POST', url }, options));
}

export function bulkApplyTemplateToProject (options) {
  const url = window.baseUrl + '/api/permissions/bulk_apply_template';
  return request(_.extend({ type: 'POST', url }, options));
}

export function addProjectCreatorToTemplate (templateName, permission) {
  const url = window.baseUrl +
      '/api/permissions/add_project_creator_to_template';
  const data = { templateName, permission };
  return post(url, data);
}

export function removeProjectCreatorFromTemplate (templateName, permission) {
  const url = window.baseUrl +
      '/api/permissions/remove_project_creator_from_template';
  const data = { templateName, permission };
  return post(url, data);
}
