function _request (options) {
  let $ = jQuery;
  return $.ajax(options);
}

function _url (path) {
  return window.baseUrl + path;
}

function _typeError (method, message) {
  throw new TypeError(`permissions#${method}: ${message}`);
}


export function getUsers (data) {
  let url = _url('/api/permissions/users');
  return _request({ type: 'GET', url: url, data: data });
}


export function grantToUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return _typeError('grantToUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return _typeError('grantToUser', 'please provide user login');
  }

  let url = _url('/api/permissions/add_user');
  let data = { permission: permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return _request({ type: 'POST', url: url, data: data });
}


export function revokeFromUser (permission, user, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return _typeError('revokeFromUser', 'please provide permission');
  }
  if (typeof user !== 'string' || !user.length) {
    return _typeError('revokeFromUser', 'please provide user login');
  }

  let url = _url('/api/permissions/remove_user');
  let data = { permission: permission, login: user };
  if (project) {
    data.projectId = project;
  }
  return _request({ type: 'POST', url: url, data: data });
}


export function getGroups (data) {
  let url = _url('/api/permissions/groups');
  return _request({ type: 'GET', url: url, data: data });
}


export function grantToGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return _typeError('grantToGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return _typeError('grantToGroup', 'please provide group name');
  }

  let url = _url('/api/permissions/add_group');
  let data = { permission: permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return _request({ type: 'POST', url: url, data: data });
}


export function revokeFromGroup (permission, group, project) {
  if (typeof permission !== 'string' || !permission.length) {
    return _typeError('revokeFromGroup', 'please provide permission');
  }
  if (typeof group !== 'string' || !group.length) {
    return _typeError('revokeFromGroup', 'please provide group name');
  }

  let url = _url('/api/permissions/remove_group');
  let data = { permission: permission, groupName: group };
  if (project) {
    data.projectId = project;
  }
  return _request({ type: 'POST', url: url, data: data });
}


export function getPermissionTemplates (query) {
  let url = _url('/api/permissions/search_templates');
  let data = { };
  if (query) {
    data.q = query;
  }
  return _request({ type: 'GET', url: url, data: data });
}


export function createPermissionTemplate (options) {
  let url = _url('/api/permissions/create_template');
  return _request(_.extend({ type: 'POST', url: url }, options));
}

export function updatePermissionTemplate (options) {
  let url = _url('/api/permissions/update_template');
  return _request(_.extend({ type: 'POST', url: url }, options));
}


export function deletePermissionTemplate (options) {
  let url = _url('/api/permissions/delete_template');
  return _request(_.extend({ type: 'POST', url: url }, options));
}


export function setDefaultPermissionTemplate (template, qualifier) {
  if (typeof template !== 'string' || !template.length) {
    return _typeError('setDefaultPermissionTemplate', 'please provide permission template ID');
  }

  let url = _url('/api/permissions/set_default_template');
  let data = { templateId: template, qualifier };
  return _request({ type: 'POST', url, data });
}


export function applyTemplateToProject(options) {
  let url = _url('/api/permissions/apply_template');
  return _request(_.extend({ type: 'POST', url: url }, options));
}
