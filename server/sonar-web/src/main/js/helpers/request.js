import _ from 'underscore';

/**
 * Default options for any request
 * @type {{credentials: string}}
 */
const OPTIONS = {
  type: 'GET',
  credentials: 'same-origin'
};


/**
 * Default request headers
 * @type {{Accept: string}}
 */
const HEADERS = {
  'Accept': 'application/json'
};


/**
 * Create a query string from an object
 * @param {object} parameters
 * @returns {string}
 */
function queryString (parameters) {
  return Object.keys(parameters)
      .map(key => {
        return `${encodeURIComponent(key)}=${encodeURIComponent(parameters[key])}`;
      })
      .join('&');
}


/**
 * Request
 */
class Request {
  constructor (url) {
    this.url = url;
    this.options = {};
    this.headers = {};
  }

  submit () {
    let url = this.url;
    let options = _.defaults(this.options, OPTIONS);
    let headers = _.defaults(this.headers, HEADERS);
    options.headers = headers;
    if (this.data) {
      if (options.type === 'GET') {
        url += '?' + queryString(this.data);
      } else {
        options.body = queryString(this.data);
      }
    }
    return window.fetch(url, options);
  }

  setData (data) {
    this.data = data;
    return this;
  }
}


/**
 * Make a request
 * @param {string} url
 * @returns {Request}
 */
export function request (url) {
  return new Request(url);
}


/**
 * Check that response status is ok
 * @param response
 * @returns {*}
 */
export function checkStatus (response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    var error = new Error(response.status);
    error.response = response;
    throw error;
  }
}


/**
 * Parse response as JSON
 * @param response
 * @returns {object}
 */
export function parseJSON (response) {
  return response.json();
}


/**
 * Shortcut to do a GET request and return response json
 * @param url
 * @param data
 */
export function getJSON (url, data) {
  return request(url)
      .setData(data)
      .submit()
      .then(checkStatus)
      .then(parseJSON);
}