function _request(options) {
  let $ = jQuery;
  return $.ajax(options);
}

function _url(path) {
  return window.baseUrl + path;
}

function _typeError(method, message) {
  throw new TypeError(`permissions#${method}: ${message}`);
}


export function getUsers(data) {
  let url = _url('/api/permissions/users');
  return _request({ type: 'GET', url: url, data: data });
}


export function grantToUser(permission, user, project) {
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


export function revokeFromUser(permission, user, project) {
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


export function getGroups(data) {
  let url = _url('/api/permissions/groups');
  return _request({ type: 'GET', url: url, data: data });
}


export function grantToGroup(permission, group, project) {
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


export function revokeFromGroup(permission, group, project) {
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


export function applyTemplateToProject(options) {
  let url = _url('/api/permissions/apply_template');
  return _request(_.extend({ type: 'POST', url: url }, options));
}
