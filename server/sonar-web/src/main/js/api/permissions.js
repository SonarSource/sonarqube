import $ from 'jquery';
import _ from 'underscore';

function request (options) {
  return $.ajax(options);
}

function buildUrl (path) {
  return window.baseUrl + path;
}

function typeError (method, message) {
  throw new TypeError(`permissions#${method}: ${message}`);
}


export function getUsers (data) {
  let url = buildUrl('/api/permissions/users');
  return request({ type: 'GET', url: url, data: data });
}


export function grantToUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('grantToUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return typeError('grantToUser', 'please provide user login');
  }

  let url = buildUrl('/api/permissions/add_user');
  let data = { permission: permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url: url, data: data });
}


export function revokeFromUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('revokeFromUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return typeError('revokeFromUser', 'please provide user login');
  }

  let url = buildUrl('/api/permissions/remove_user');
  let data = { permission: permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url: url, data: data });
}


export function getGroups (data) {
  let url = buildUrl('/api/permissions/groups');
  return request({ type: 'GET', url: url, data: data });
}


export function grantToGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('grantToGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return typeError('grantToGroup', 'please provide group name');
  }

  let url = buildUrl('/api/permissions/add_group');
  let data = { permission: permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url: url, data: data });
}


export function revokeFromGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return typeError('revokeFromGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return typeError('revokeFromGroup', 'please provide group name');
  }

  let url = buildUrl('/api/permissions/remove_group');
  let data = { permission: permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return request({ type: 'POST', url: url, data: data });
}


export function getPermissionTemplates (query) {
  let url = buildUrl('/api/permissions/search_templates');
  let data = { };
  if (query) {
    data.q = query;
  }
  return request({ type: 'GET', url: url, data: data });
}


export function createPermissionTemplate (options) {
  let url = buildUrl('/api/permissions/create_template');
  return request(_.extend({ type: 'POST', url: url }, options));
}

export function updatePermissionTemplate (options) {
  let url = buildUrl('/api/permissions/update_template');
  return request(_.extend({ type: 'POST', url: url }, options));
}


export function deletePermissionTemplate (options) {
  let url = buildUrl('/api/permissions/delete_template');
  return request(_.extend({ type: 'POST', url: url }, options));
}


export function setDefaultPermissionTemplate (template, qualifier) {
  if (typeof template !== 'string' || !template.length) {
    return typeError('setDefaultPermissionTemplate', 'please provide permission template ID');
  }

  let url = buildUrl('/api/permissions/set_default_template');
  let data = { templateId: template, qualifier };
  return request({ type: 'POST', url, data });
}


export function applyTemplateToProject(options) {
  let url = buildUrl('/api/permissions/apply_template');
  return request(_.extend({ type: 'POST', url: url }, options));
}
