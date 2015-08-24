function _request(options) {
  let $ = jQuery;
  return $.ajax(options);
}

function _url(path) {
  return window.baseUrl + path;
}

function _typeError(method, message) {
  throw new TypeError(`navigation#${method}: ${message}`);
}

export function global() {
  let url = _url('/api/navigation/global');
  return _request({ type: 'GET', url });
}

export function component(componentKey) {
  if (typeof componentKey !== 'string' || !componentKey.length) {
    return _typeError('component', 'please provide componentKey');
  }
  let url = _url('/api/navigation/component');
  let data = { componentKey };
  return _request({ type: 'GET', url, data });
}
