function _request(options) {
  let $ = jQuery;
  return $.ajax(options);
}

function _url(path) {
  return window.baseUrl + path;
}

export function getCurrentUser() {
  let url = _url('/api/users/current');
  return _request({ type: 'GET', url });
}
